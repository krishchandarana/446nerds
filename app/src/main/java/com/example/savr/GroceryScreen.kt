package com.savr.app.ui.screens.grocery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.savr.data.GROCERY_SOURCE_RECIPE
import com.example.savr.data.GROCERY_SOURCE_USER
import com.example.savr.data.UserProfile
import com.example.savr.data.deserializeGroceryItem
import com.example.savr.data.getCategoryFromSerialized
import com.example.savr.data.getGrocerySource
import com.example.savr.data.mergeSerializedGroceryItems
import com.example.savr.data.setUserProfile
import com.example.savr.data.userProfileFlow
import com.savr.app.ui.GroceryItem
import com.savr.app.ui.components.PageHeader
import com.savr.app.ui.theme.SavrColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Required categories that should always be displayed
private val REQUIRED_CATEGORIES = listOf(
    "Fruit",
    "Produce",
    "Spices",
    "Bakery",
    "Beverages",
    "Dairy & Eggs",
    "Meat",
    "Condiments",
    "Pantry",
    "Baking"
)

private data class GroceryDisplayItem(
    val category: String,
    val item: GroceryItem,
    val rawIndices: List<Int>,
    val isChecked: Boolean,
    val sourceNote: String
)

private data class GroceryDisplayBucket(
    val category: String,
    val emoji: String,
    val name: String,
    val unit: String,
    var quantity: Int,
    val rawIndices: MutableList<Int>,
    var allChecked: Boolean,
    val sources: MutableSet<String>
)

private fun normalizeGroceryName(value: String): String {
    return value.trim()
        .lowercase()
        .replace("_", " ")
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun normalizeGroceryUnit(value: String): String {
    return value.trim().lowercase().removeSuffix("s")
}

private fun parseGroceryQuantityLabel(quantity: String): Pair<Int, String>? {
    val match = Regex("""^(\d+)\s*(.*)$""").find(quantity.trim()) ?: return null
    val amount = match.groupValues[1].toIntOrNull() ?: return null
    return amount to normalizeGroceryUnit(match.groupValues[2])
}

private fun buildSourceNote(sources: Set<String>): String {
    val hasUser = GROCERY_SOURCE_USER in sources
    val hasRecipe = GROCERY_SOURCE_RECIPE in sources
    return when {
        hasUser && hasRecipe -> "Added manually and missing from recipe"
        hasRecipe -> "Missing recipe ingredient"
        else -> "Added manually"
    }
}

private fun buildCombinedGroceryItems(items: List<String>): Map<String, List<GroceryDisplayItem>> {
    val buckets = linkedMapOf<String, GroceryDisplayBucket>()

    items.forEachIndexed { index, serialized ->
        val baseSerialized = serialized.removeSuffix("|checked")
        val parsedItem = deserializeGroceryItem(baseSerialized, index, isChecked = serialized.endsWith("|checked"))
            ?: return@forEachIndexed
        val category = getCategoryFromSerialized(baseSerialized) ?: "Other"
        val source = getGrocerySource(baseSerialized)
        val quantityParts = parseGroceryQuantityLabel(parsedItem.quantity)
        val unit = quantityParts?.second.orEmpty()
        val quantityValue = quantityParts?.first ?: 1
        val key = "${normalizeGroceryName(parsedItem.name)}|${category.lowercase()}|$unit"
        val isChecked = serialized.endsWith("|checked")

        val existing = buckets[key]
        if (existing == null) {
            buckets[key] = GroceryDisplayBucket(
                category = category,
                emoji = parsedItem.emoji,
                name = parsedItem.name,
                unit = unit,
                quantity = quantityValue,
                rawIndices = mutableListOf(index),
                allChecked = isChecked,
                sources = mutableSetOf(source)
            )
        } else {
            existing.quantity += quantityValue
            existing.rawIndices.add(index)
            existing.allChecked = existing.allChecked && isChecked
            existing.sources.add(source)
        }
    }

    return buckets.values
        .map { bucket ->
            val quantityLabel = listOf(bucket.quantity.toString(), bucket.unit)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            GroceryDisplayItem(
                category = bucket.category,
                item = GroceryItem(
                    id = bucket.rawIndices.first(),
                    emoji = bucket.emoji,
                    name = bucket.name,
                    quantity = quantityLabel.ifBlank { bucket.quantity.toString() },
                    isChecked = bucket.allChecked
                ),
                rawIndices = bucket.rawIndices.toList(),
                isChecked = bucket.allChecked,
                sourceNote = buildSourceNote(bucket.sources)
            )
        }
        .groupBy { it.category }
}

// Category emoji mapping
private fun getCategoryEmoji(category: String): String {
    return when (category.lowercase()) {
        "fruit" -> "🍎"
        "produce" -> "🥬"
        "spices" -> "🌶️"
        "bakery" -> "🥖"
        "beverages" -> "🥤"
        "dairy & eggs", "dairy", "eggs" -> "🥛"
        "meat" -> "🥩"
        "condiments" -> "🍯"
        "pantry" -> "🥫"
        "baking" -> "🧁"
        "vegetables", "vegetable" -> "🥦"
        "protein" -> "🥩"
        "grain", "grains" -> "🌾"
        else -> "🥄"
    }
}
@Composable
fun GroceryScreen() {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        userProfileFlow().collectLatest { 
            profile = it
        }
    }

    if (showAddSheet) {
        AddGroceryItemSheet(
            onSave = { serializedItems ->
                val currentProfile = profile
                if (currentProfile != null) {
                    val updatedList = mergeSerializedGroceryItems(currentProfile.groceryList + serializedItems)
                    val updatedProfile = currentProfile.copy(groceryList = updatedList)
                    profile = updatedProfile
                    scope.launch {
                        try {
                            setUserProfile(updatedProfile)
                            showAddSheet = false
                        } catch (e: Exception) {
                            android.util.Log.e("GroceryScreen", "Failed to save grocery item", e)
                        }
                    }
                } else {
                    showAddSheet = false
                }
            },
            onDismiss = { showAddSheet = false }
        )
    }

    // Group items by category
    val itemsByCategory = remember(profile?.groceryList) {
        buildCombinedGroceryItems(profile?.groceryList ?: emptyList())
    }

    // Create a map that includes all required categories, even if empty
    val allCategoriesWithItems = remember(itemsByCategory) {
        val result = mutableMapOf<String, List<GroceryDisplayItem>>()
        // Add all required categories first
        REQUIRED_CATEGORIES.forEach { category ->
            result[category] = itemsByCategory[category] ?: emptyList()
        }
        // Add any other categories that have items but aren't in the required list
        itemsByCategory.forEach { (category, items) ->
            if (category !in REQUIRED_CATEGORIES) {
                result[category] = items
            }
        }
        result
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SavrColors.Cream)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 90.dp)
        ) {
            PageHeader("Grocery List", subtitle = "")

            allCategoriesWithItems.forEach { (category, items) ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 8.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(getCategoryEmoji(category), fontSize = 14.sp)
                        Text(
                            text = category.uppercase(),
                            color = SavrColors.TextMid,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    HorizontalDivider(
                        color = SavrColors.DividerColour,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }

                items.forEach { displayItem ->
                    GroceryItemRow(
                        item = displayItem.item,
                        isChecked = displayItem.isChecked,
                        sourceNote = displayItem.sourceNote,
                        onToggle = {
                            val currentProfile = profile
                            if (currentProfile != null) {
                                val updatedList = currentProfile.groceryList.toMutableList()
                                displayItem.rawIndices.forEach { rawIndex ->
                                    val serialized = updatedList[rawIndex].replace("|checked", "")
                                    updatedList[rawIndex] = if (displayItem.isChecked) {
                                        serialized
                                    } else {
                                        "$serialized|checked"
                                    }
                                }
                                val updatedProfile = currentProfile.copy(groceryList = updatedList)
                                profile = updatedProfile
                                scope.launch {
                                    try {
                                        setUserProfile(updatedProfile)
                                    } catch (e: Exception) {
                                        android.util.Log.e("GroceryScreen", "Failed to update checked state", e)
                                    }
                                }
                            }
                        },
                        onDelete = {
                            val currentProfile = profile
                            if (currentProfile != null) {
                                val updatedList = currentProfile.groceryList.toMutableList()
                                displayItem.rawIndices
                                    .sortedDescending()
                                    .forEach { rawIndex -> updatedList.removeAt(rawIndex) }
                                val updatedProfile = currentProfile.copy(groceryList = updatedList)
                                profile = updatedProfile
                                scope.launch {
                                    try {
                                        setUserProfile(updatedProfile)
                                    } catch (e: Exception) {
                                        android.util.Log.e("GroceryScreen", "Failed to delete grocery item", e)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 88.dp)
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SavrColors.Dark)
                .clickable { showAddSheet = true },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = SavrColors.White, fontSize = 22.sp)
        }
    }
}


@Composable
private fun GroceryItemRow(
    item: GroceryItem,
    isChecked: Boolean,
    sourceNote: String,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SavrColors.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isChecked) SavrColors.Sage else SavrColors.CreamMid)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Text("✓", color = SavrColors.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // Delete button
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(SavrColors.TerraTint)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = SavrColors.Terra, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            
            Text(item.emoji, fontSize = 18.sp)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggle() }
            ) {
                Text(
                    text           = item.name,
                    color          = if (isChecked) SavrColors.TextMuted else SavrColors.Dark,
                    fontSize       = 14.sp,
                    fontWeight     = FontWeight.Medium,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = sourceNote,
                    color = SavrColors.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Text(
                text       = item.quantity,
                color      = SavrColors.TextMuted,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium
            )

        }

    }

}
