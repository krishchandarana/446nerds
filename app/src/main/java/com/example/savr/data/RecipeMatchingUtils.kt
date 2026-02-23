package com.example.savr.data

import com.example.savr.data.deserializeInventoryItem
import com.savr.app.ui.CurrentInventoryItem
import com.savr.app.ui.ExpiryStatus
import com.savr.app.ui.Recipe
import com.savr.app.ui.theme.SavrColors
import androidx.compose.ui.graphics.Color

/**
 * Scores a recipe based on ingredient availability and expiry urgency
 * Higher score = better match (uses more expiring ingredients)
 * 
 * Scoring:
 * - Base score: number of matched ingredients
 * - Bonus: URGENT items = +1000, WARNING items = +100, FRESH items = +10
 * - Quantity doesn't matter, just presence
 */
fun scoreRecipe(
    recipe: RecipeCatalogItem,
    inventoryItemsByName: Map<String, CurrentInventoryItem>
): Double {
    if (recipe.ingredients.isEmpty()) {
        return 0.0
    }
    
    var score = 0.0
    var matchedCount = 0
    
    recipe.ingredients.forEach { ingredient ->
        val foodIdLower = ingredient.foodId.lowercase()
        val inventoryItem = inventoryItemsByName[foodIdLower]
        
        if (inventoryItem != null) {
            matchedCount++
            // Add bonus based on expiry status (prioritize expiring items)
            when (inventoryItem.status) {
                ExpiryStatus.URGENT -> score += 1000.0
                ExpiryStatus.WARNING -> score += 100.0
                ExpiryStatus.FRESH -> score += 10.0
            }
        }
    }
    
    // Base score: percentage of ingredients matched (0-1) * 100
    val matchPercentage = if (recipe.ingredients.isNotEmpty()) {
        matchedCount.toDouble() / recipe.ingredients.size
    } else {
        0.0
    }
    
    // Combine: match percentage + expiry bonuses
    return (matchPercentage * 100) + score
}

/**
 * Filters and ranks recipes by ingredient availability and expiry urgency
 * Returns top 7 recipes that use ingredients with closest expiration dates
 */
fun filterRecipesByInventory(
    recipes: List<RecipeCatalogItem>,
    inventoryItems: List<CurrentInventoryItem>
): List<RecipeCatalogItem> {
    // Create a map for quick lookup: item name (lowercase) -> inventory item
    val inventoryItemsByName = inventoryItems.associateBy { it.name.lowercase() }
    
    // Score all recipes and filter out those with no matches
    val scoredRecipes = recipes.mapNotNull { recipe ->
        val score = scoreRecipe(recipe, inventoryItemsByName)
        if (score > 0) {
            Pair(recipe, score)
        } else {
            null
        }
    }
    
    // Sort by score (highest first) and take top 7
    return scoredRecipes
        .sortedByDescending { it.second }
        .take(7)
        .map { it.first }
}

/**
 * Maps a RecipeCatalogItem from the database to a Recipe for display
 */
fun mapRecipeCatalogToRecipe(
    catalogItem: RecipeCatalogItem,
    isSelected: Boolean = false,
    inventoryItems: List<CurrentInventoryItem>? = null
): Recipe {
    // Count matched ingredients if inventory is provided
    val matchedCount = if (inventoryItems != null) {
        val inventoryItemsByName = inventoryItems.associateBy { it.name.lowercase() }
        catalogItem.ingredients.count { ingredient ->
            inventoryItemsByName.containsKey(ingredient.foodId.lowercase())
        }
    } else {
        catalogItem.ingredients.size
    }
    
    // Determine badge based on match count
    val matchBadge = when {
        matchedCount == catalogItem.ingredients.size -> "✓ All ingredients"
        matchedCount > 0 -> "✓ $matchedCount/${catalogItem.ingredients.size} ingredients"
        else -> "✓ Available"
    }
    
    // Use Sage color for matched recipes
    val badgeColor = SavrColors.Sage
    val gradientStart = Color(0xFFEBF3EC)
    val gradientEnd = Color(0x667A9E7E)
    
    return Recipe(
        id = catalogItem.id, // Use the Firestore document ID
        emoji = catalogItem.emoji,
        name = catalogItem.title,
        calories = catalogItem.calories,
        minutes = catalogItem.prepTimeMinutes,
        matchBadge = matchBadge,
        badgeColor = badgeColor,
        gradientStart = gradientStart,
        gradientEnd = gradientEnd,
        isSelected = isSelected
    )
}
