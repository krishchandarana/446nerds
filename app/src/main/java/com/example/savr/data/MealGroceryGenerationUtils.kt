package com.example.savr.data

import com.savr.app.ui.CurrentInventoryItem
import com.savr.app.ui.GroceryItem

private data class GroceryMergeBucket(
    val emoji: String,
    val name: String,
    val category: String,
    val unit: String,
    val source: String,
    var quantity: Int,
    var checked: Boolean
)

private fun normalizeName(value: String): String {
    return value.trim()
        .lowercase()
        .replace("_", " ")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun normalizeTokenKey(value: String): String {
    return normalizeName(value)
        .split(" ")
        .filter { it.isNotBlank() }
        .sorted()
        .joinToString(" ")
}

private fun normalizeUnitForMerge(value: String): String = value.trim().lowercase().removeSuffix("s")

private fun parseQuantityLabel(quantity: String): Pair<Int, String>? {
    val match = Regex("""^(\d+)\s*(.*)$""").find(quantity.trim()) ?: return null
    val amount = match.groupValues[1].toIntOrNull() ?: return null
    val unit = normalizeUnitForMerge(match.groupValues[2])
    return amount to unit
}

private fun getAvailableInventoryQuantity(
    foodName: String,
    unit: String,
    inventoryItems: List<CurrentInventoryItem>
): Int {
    val normalizedName = normalizeName(foodName)
    val normalizedTokenKey = normalizeTokenKey(foodName)
    val normalizedUnit = normalizeUnitForMerge(unit)

    return inventoryItems
        .filter {
            val itemName = normalizeName(it.name)
            itemName == normalizedName || normalizeTokenKey(it.name) == normalizedTokenKey
        }
        .filterNot(::isInventoryItemExpired)
        .sumOf { item ->
            val parsed = parseQuantityLabel(item.quantity) ?: return@sumOf 1
            val itemUnit = parsed.second
            if (normalizedUnit.isBlank() || itemUnit.isBlank() || itemUnit == normalizedUnit) {
                parsed.first
            } else {
                0
            }
        }
}

private fun bucketKey(name: String, category: String, unit: String): String {
    return bucketKey(name, category, unit, GROCERY_SOURCE_USER)
}

private fun bucketKey(name: String, category: String, unit: String, source: String): String {
    return "${normalizeName(name)}|${normalizeName(category)}|${normalizeUnitForMerge(unit)}|${source.trim().lowercase()}"
}

private fun bucketizeSerializedGroceryEntries(items: List<String>): LinkedHashMap<String, GroceryMergeBucket> {
    val merged = linkedMapOf<String, GroceryMergeBucket>()

    items.forEach { serialized ->
        val baseSerialized = serialized.removeSuffix("|checked")
        val parsed = deserializeGroceryItem(baseSerialized, id = 0, isChecked = serialized.endsWith("|checked"))
            ?: return@forEach
        val category = getCategoryFromSerialized(baseSerialized).orEmpty()
        val source = getGrocerySource(baseSerialized)
        val quantityParts = parseQuantityLabel(parsed.quantity) ?: return@forEach
        val checked = serialized.endsWith("|checked")
        val key = bucketKey(parsed.name, category, quantityParts.second, source)

        val existing = merged[key]
        if (existing == null) {
            merged[key] = GroceryMergeBucket(
                emoji = parsed.emoji,
                name = parsed.name,
                category = category,
                unit = quantityParts.second,
                source = source,
                quantity = quantityParts.first,
                checked = checked
            )
        } else {
            existing.quantity += quantityParts.first
            existing.checked = existing.checked || checked
        }
    }

    return merged
}

private fun serializeBuckets(buckets: Collection<GroceryMergeBucket>): List<String> {
    return buckets.map { bucket ->
        val quantityLabel = listOf(bucket.quantity.toString(), bucket.unit).filter { it.isNotBlank() }.joinToString(" ")
        val serialized = serializeGroceryItem(
            item = GroceryItem(
                id = 0,
                emoji = bucket.emoji,
                name = bucket.name,
                quantity = quantityLabel
            ),
            category = bucket.category,
            source = bucket.source
        )
        if (bucket.checked) "$serialized|checked" else serialized
    }
}

private fun mergeSerializedGroceryEntries(items: List<String>): List<String> {
    return serializeBuckets(bucketizeSerializedGroceryEntries(items).values)
}

private fun resolveFoodCatalogItem(
    foodId: String,
    foodCatalogItems: List<FoodCatalogItem>
): FoodCatalogItem? {
    val normalizedName = normalizeName(foodId)
    val tokenKey = normalizeTokenKey(foodId)

    return foodCatalogItems.firstOrNull { normalizeName(it.name) == normalizedName }
        ?: foodCatalogItems.firstOrNull { normalizeTokenKey(it.name) == tokenKey }
        ?: foodCatalogItems.firstOrNull { catalogItem ->
            val catalogName = normalizeName(catalogItem.name)
            catalogName.contains(normalizedName) || normalizedName.contains(catalogName)
        }
}

private fun prettifyFoodId(foodId: String): String {
    return normalizeName(foodId)
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
}

fun buildMergedGroceryListForRecipes(
    existingGroceryList: List<String>,
    plannedRecipeIds: Set<String>,
    recipeCatalog: List<RecipeCatalogItem>,
    inventoryItems: List<CurrentInventoryItem>,
    foodCatalogItems: List<FoodCatalogItem>
): List<String> {
    if (plannedRecipeIds.isEmpty()) {
        return existingGroceryList.filter { getGrocerySource(it.removeSuffix("|checked")) != GROCERY_SOURCE_RECIPE }
    }

    val generatedItems = recipeCatalog
        .filter { it.id in plannedRecipeIds }
        .flatMap { recipe -> recipe.ingredients }
        .mapNotNull { ingredient ->
            val availableQuantity = getAvailableInventoryQuantity(
                foodName = ingredient.foodId,
                unit = ingredient.unit,
                inventoryItems = inventoryItems
            )
            val missingQuantity = ingredient.quantity.coerceAtLeast(1) - availableQuantity
            if (missingQuantity <= 0) return@mapNotNull null

            val foodCatalogItem = resolveFoodCatalogItem(ingredient.foodId, foodCatalogItems)
            val category = foodCatalogItem?.category ?: "Other"
            val unit = ingredient.unit.ifBlank { foodCatalogItem?.defaultUnit.orEmpty() }
            val quantityLabel = listOf(missingQuantity.toString(), unit.trim()).filter { it.isNotBlank() }.joinToString(" ")
            serializeGroceryItem(
                item = GroceryItem(
                    id = 0,
                    emoji = foodCatalogItem?.emoji ?: "🥄",
                    name = foodCatalogItem?.name ?: prettifyFoodId(ingredient.foodId),
                    quantity = quantityLabel.ifBlank { missingQuantity.toString() }
                ),
                category = category,
                source = GROCERY_SOURCE_RECIPE
            )
        }

    val existingBuckets = bucketizeSerializedGroceryEntries(
        existingGroceryList.filter { getGrocerySource(it.removeSuffix("|checked")) != GROCERY_SOURCE_RECIPE }
    )
    val generatedBuckets = bucketizeSerializedGroceryEntries(generatedItems)

    generatedBuckets.forEach { (key, generatedBucket) ->
        val existingBucket = existingBuckets[key]
        if (existingBucket == null) {
            existingBuckets[key] = generatedBucket
        } else if (generatedBucket.quantity > existingBucket.quantity) {
            existingBucket.quantity = generatedBucket.quantity
        }
    }

    return serializeBuckets(existingBuckets.values)
}

fun mergeSerializedGroceryItems(items: List<String>): List<String> {
    return mergeSerializedGroceryEntries(items)
}
