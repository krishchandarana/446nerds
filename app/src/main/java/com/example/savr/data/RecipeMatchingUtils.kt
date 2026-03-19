package com.example.savr.data

import androidx.compose.ui.graphics.Color
import com.savr.app.ui.CurrentInventoryItem
import com.savr.app.ui.ExpiryStatus
import com.savr.app.ui.Recipe
import com.savr.app.ui.theme.SavrColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val inventoryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun normalizeComparableName(value: String): String {
    return value.trim()
        .lowercase()
        .replace("_", " ")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun normalizeComparableTokenKey(value: String): String {
    return normalizeComparableName(value)
        .split(" ")
        .filter { it.isNotBlank() }
        .sorted()
        .joinToString(" ")
}

private data class InventoryIngredientMatch(
    val availableQuantity: Int,
    val requiredQuantity: Int,
    val matchedItemCount: Int,
    val nearestStatus: ExpiryStatus?
)

private fun normalizeUnit(unit: String): String {
    return unit.trim().lowercase().removeSuffix("s")
}

private fun parseInventoryQuantity(quantity: String): Pair<Int, String>? {
    val match = Regex("""^(\d+)\s*(.*)$""").find(quantity.trim()) ?: return null
    val amount = match.groupValues[1].toIntOrNull() ?: return null
    val unit = normalizeUnit(match.groupValues[2])
    return amount to unit
}

private fun parseExpiryDateOrNull(value: String): LocalDate? {
    return try {
        LocalDate.parse(value.trim(), inventoryDateFormatter)
    } catch (_: Exception) {
        null
    }
}

fun isInventoryItemExpired(item: CurrentInventoryItem): Boolean {
    val expiryDate = parseExpiryDateOrNull(item.expiryLabel) ?: return false
    return expiryDate.isBefore(LocalDate.now())
}

private fun recipeSatisfiesDiet(
    recipe: RecipeCatalogItem,
    dietaryPreferences: List<String>
): Boolean {
    if (dietaryPreferences.isEmpty()) return true

    val normalizedRestrictions = recipe.dietaryRestrictions.map { it.trim().lowercase() }
    val normalizedFlags = recipe.dietaryFlags.mapKeys { it.key.trim().lowercase() }

    return dietaryPreferences.all { preference ->
        when (preference.trim().lowercase()) {
            "vegetarian" -> normalizedFlags["vegetarian"] == true ||
                normalizedRestrictions.contains("vegetarian")
            "lactose free" -> normalizedFlags["lactosefree"] == true ||
                normalizedFlags["lactose_free"] == true ||
                normalizedRestrictions.contains("lactose free")
            "peanut free" -> normalizedFlags["peanutfree"] == true ||
                normalizedFlags["peanut_free"] == true ||
                normalizedRestrictions.contains("peanut free")
            else -> true
        }
    }
}

private fun findIngredientMatch(
    ingredient: RecipeIngredient,
    inventoryItems: List<CurrentInventoryItem>
): InventoryIngredientMatch {
    val ingredientName = normalizeComparableName(ingredient.foodId)
    val ingredientTokenKey = normalizeComparableTokenKey(ingredient.foodId)
    val matchingItems = inventoryItems.filter {
        val inventoryName = normalizeComparableName(it.name)
        (inventoryName == ingredientName || normalizeComparableTokenKey(it.name) == ingredientTokenKey) &&
            !isInventoryItemExpired(it)
    }

    if (matchingItems.isEmpty()) {
        return InventoryIngredientMatch(
            availableQuantity = 0,
            requiredQuantity = ingredient.quantity.coerceAtLeast(1),
            matchedItemCount = 0,
            nearestStatus = null
        )
    }

    val requiredQuantity = ingredient.quantity.coerceAtLeast(1)
    val normalizedRequiredUnit = normalizeUnit(ingredient.unit)
    var availableQuantity = 0

    matchingItems.forEach { item ->
        val parsed = parseInventoryQuantity(item.quantity)
        if (parsed != null) {
            val (qty, unit) = parsed
            if (normalizedRequiredUnit.isBlank() || unit.isBlank() || unit == normalizedRequiredUnit) {
                availableQuantity += qty
            }
        } else {
            availableQuantity += 1
        }
    }

    val nearestStatus = matchingItems.minByOrNull {
        parseExpiryDateOrNull(it.expiryLabel) ?: LocalDate.MAX
    }?.status

    return InventoryIngredientMatch(
        availableQuantity = availableQuantity,
        requiredQuantity = requiredQuantity,
        matchedItemCount = matchingItems.size,
        nearestStatus = nearestStatus
    )
}

fun scoreRecipe(
    recipe: RecipeCatalogItem,
    inventoryItems: List<CurrentInventoryItem>,
    dietaryPreferences: List<String>
): Double {
    if (recipe.ingredients.isEmpty() || !recipeSatisfiesDiet(recipe, dietaryPreferences)) {
        return 0.0
    }

    var score = 0.0
    var coveredIngredients = 0
    var partialIngredients = 0

    recipe.ingredients.forEach { ingredient ->
        val match = findIngredientMatch(ingredient, inventoryItems)
        if (match.availableQuantity <= 0) return@forEach

        if (match.availableQuantity >= match.requiredQuantity) {
            coveredIngredients++
            score += 100.0
        } else {
            partialIngredients++
            score += 40.0 * (match.availableQuantity.toDouble() / match.requiredQuantity.toDouble())
        }

        score += when (match.nearestStatus) {
            ExpiryStatus.URGENT -> 80.0
            ExpiryStatus.WARNING -> 25.0
            ExpiryStatus.FRESH -> 5.0
            null -> 0.0
        }
    }

    val coverageRatio = (coveredIngredients + (partialIngredients * 0.5)) / recipe.ingredients.size.toDouble()
    return score + (coverageRatio * 150.0)
}

fun filterRecipesByInventory(
    recipes: List<RecipeCatalogItem>,
    inventoryItems: List<CurrentInventoryItem>,
    dietaryPreferences: List<String> = emptyList()
): List<RecipeCatalogItem> {
    val usableInventory = inventoryItems.filterNot(::isInventoryItemExpired)
    fun rankCandidates(
        candidates: List<RecipeCatalogItem>,
        enforcedDietaryPreferences: List<String>
    ): List<Pair<RecipeCatalogItem, Double>> {
        return candidates
            .map { recipe ->
                recipe to scoreRecipe(recipe, usableInventory, enforcedDietaryPreferences)
            }
            .sortedWith(
                compareByDescending<Pair<RecipeCatalogItem, Double>> { it.second }
                    .thenBy { it.first.ingredients.size }
                    .thenBy { it.first.prepTimeMinutes }
            )
    }

    val selectedRecipes = linkedSetOf<RecipeCatalogItem>()

    val strictRanked = rankCandidates(
        candidates = recipes.filter { recipeSatisfiesDiet(it, dietaryPreferences) },
        enforcedDietaryPreferences = dietaryPreferences
    )

    strictRanked
        .filter { it.second > 0.0 }
        .forEach { (recipe, _) ->
            if (selectedRecipes.size < 7) selectedRecipes.add(recipe)
        }

    strictRanked
        .filter { it.second <= 0.0 }
        .forEach { (recipe, _) ->
            if (selectedRecipes.size < 7) selectedRecipes.add(recipe)
        }

    if (selectedRecipes.size < 7) {
        rankCandidates(
            candidates = recipes.filter { candidate -> selectedRecipes.none { it.id == candidate.id } },
            enforcedDietaryPreferences = emptyList()
        ).forEach { (recipe, _) ->
            if (selectedRecipes.size < 7) selectedRecipes.add(recipe)
        }
    }

    return selectedRecipes.toList().take(7)
}

fun mapRecipeCatalogToRecipe(
    catalogItem: RecipeCatalogItem,
    isSelected: Boolean = false,
    inventoryItems: List<CurrentInventoryItem>? = null
): Recipe {
    val usableInventory = inventoryItems?.filterNot(::isInventoryItemExpired).orEmpty()
    val ingredientMatches = catalogItem.ingredients.map { ingredient ->
        findIngredientMatch(ingredient, usableInventory)
    }
    val matchedCount = ingredientMatches.count { it.availableQuantity > 0 }
    val expiringCount = ingredientMatches.count {
        it.availableQuantity > 0 && (it.nearestStatus == ExpiryStatus.URGENT || it.nearestStatus == ExpiryStatus.WARNING)
    }

    val matchBadge = when {
        expiringCount > 0 -> "⚡ Uses $expiringCount near-expiry item${if (expiringCount == 1) "" else "s"}"
        matchedCount == catalogItem.ingredients.size && matchedCount > 0 -> "✓ All ingredients"
        matchedCount > 0 -> "✓ $matchedCount/${catalogItem.ingredients.size} ingredients"
        else -> "✓ Available"
    }

    return Recipe(
        id = catalogItem.id,
        emoji = catalogItem.emoji,
        name = catalogItem.title,
        calories = catalogItem.calories,
        minutes = catalogItem.prepTimeMinutes,
        matchBadge = matchBadge,
        badgeColor = if (expiringCount > 0) SavrColors.Terra else SavrColors.Sage,
        gradientStart = Color(0xFFEBF3EC),
        gradientEnd = Color(0x667A9E7E),
        isSelected = isSelected
    )
}
