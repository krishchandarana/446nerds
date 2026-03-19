package com.savr.app.ui.screens.CurrentInventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.savr.data.UserProfile
import com.example.savr.data.buildMergedGroceryListForRecipes
import com.example.savr.data.deserializeInventoryItem
import com.example.savr.data.deserializeRecipes
import com.example.savr.data.filterRecipesByInventory
import com.example.savr.data.getAllFoodCatalogItems
import com.example.savr.data.getAllRecipes
import com.example.savr.data.getCategoryFromInventorySerialized
import com.example.savr.data.mapRecipeCatalogToRecipe
import com.example.savr.data.serializeRecipes
import com.example.savr.data.setUserProfile
import com.example.savr.data.userProfileFlow
import com.savr.app.ui.*
import com.savr.app.ui.components.*
import com.savr.app.ui.theme.SavrColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Required categories that should always be displayed (same as grocery)
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

// Category emoji mapping (same as GroceryScreen)
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

private data class InventoryBatch(
    val item: CurrentInventoryItem,
    val index: Int
)

private data class InventoryGroup(
    val key: String,
    val name: String,
    val emoji: String,
    val totalQuantityLabel: String,
    val earliestExpiryLabel: String,
    val earliestStatus: ExpiryStatus,
    val batches: List<InventoryBatch>
)

private fun parseInventoryDateOrNull(value: String): LocalDate? {
    return try {
        LocalDate.parse(value.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (_: Exception) {
        null
    }
}

private fun summarizeQuantities(items: List<CurrentInventoryItem>): String {
    val parsed = items.mapNotNull { item ->
        val match = Regex("""^(\d+)\s*(.*)$""").find(item.quantity.trim()) ?: return@mapNotNull null
        val amount = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
        amount to match.groupValues[2].trim()
    }

    if (parsed.size != items.size || parsed.isEmpty()) {
        return "${items.size} batches"
    }

    val distinctUnits = parsed.map { it.second.lowercase() }.distinct()
    if (distinctUnits.size != 1) {
        return "${items.size} batches"
    }

    val total = parsed.sumOf { it.first }
    val unit = parsed.first().second
    return listOf(total.toString(), unit).filter { it.isNotBlank() }.joinToString(" ")
}

private fun buildInventoryGroups(
    category: String,
    batches: List<InventoryBatch>
): List<InventoryGroup> {
    return batches
        .groupBy { it.item.name.lowercase() }
        .map { (_, groupedBatches) ->
            val sortedBatches = groupedBatches.sortedWith(
                compareBy<InventoryBatch> { parseInventoryDateOrNull(it.item.expiryLabel) ?: LocalDate.MAX }
                    .thenBy { it.item.quantity }
            )
            val earliestBatch = sortedBatches.first()
            InventoryGroup(
                key = "$category|${earliestBatch.item.name.lowercase()}",
                name = earliestBatch.item.name,
                emoji = earliestBatch.item.emoji,
                totalQuantityLabel = summarizeQuantities(sortedBatches.map { it.item }),
                earliestExpiryLabel = earliestBatch.item.expiryLabel,
                earliestStatus = earliestBatch.item.status,
                batches = sortedBatches
            )
        }
        .sortedBy { parseInventoryDateOrNull(it.earliestExpiryLabel) ?: LocalDate.MAX }
}

@Composable
fun CurrentInventoryScreen(onNavigateToMeals: (List<Recipe>) -> Unit) {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        userProfileFlow().collectLatest { profile = it }
    }

    if (showAddSheet) {
        AddCurrentInventoryItemSheet(
            onSave = { serializedItems ->
                val currentProfile = profile
                if (currentProfile != null) {
                    val updatedList = currentProfile.currentInventory + serializedItems
                    val updatedProfile = currentProfile.copy(currentInventory = updatedList)
                    scope.launch {
                        try {
                            setUserProfile(updatedProfile)
                            showAddSheet = false
                        } catch (e: Exception) {
                            android.util.Log.e("CurrentInventoryScreen", "Failed to save inventory item", e)
                        }
                    }
                } else {
                    showAddSheet = false
                }
            },
            onDismiss = { showAddSheet = false }
        )
    }

    // Group items by database category (same as grocery)
    val itemsByCategory = remember(profile?.currentInventory) {
        val items = profile?.currentInventory ?: emptyList()
        items.mapIndexedNotNull { index, serialized ->
            val item = deserializeInventoryItem(serialized, index)
            val category = getCategoryFromInventorySerialized(serialized) ?: "Other"
            if (item != null) {
                category to InventoryBatch(item = item, index = index)
            } else {
                null
            }
        }.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )
    }

    // Create a map that includes all required categories, even if empty
    val allCategoriesWithItems = remember(itemsByCategory) {
        val result = mutableMapOf<String, List<InventoryGroup>>()
        // Add all required categories first
        REQUIRED_CATEGORIES.forEach { category ->
            result[category] = buildInventoryGroups(category, itemsByCategory[category] ?: emptyList())
        }
        // Add any other categories that have items but aren't in the required list
        itemsByCategory.forEach { (category, items) ->
            if (category !in REQUIRED_CATEGORIES) {
                result[category] = buildInventoryGroups(category, items)
            }
        }
        result
    }
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    Box(modifier = Modifier.fillMaxSize().background(SavrColors.Cream)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            PageHeader(title = "My Current Inventory", subtitle = "")

            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SavrColors.SageTint)
                    .clickable {
                        // Fetch recipes and filter by inventory
                        scope.launch {
                            try {
                                val allRecipes = getAllRecipes()
                                val foodCatalogItems = getAllFoodCatalogItems()
                                val currentProfile = profile
                                // Extract all inventory items from the grouped categories
                                val inventoryItems = allCategoriesWithItems.values
                                    .flatten()
                                    .flatMap { group -> group.batches.map { it.item } }
                                android.util.Log.d("CurrentInventoryScreen", "Inventory items: ${inventoryItems.map { it.name }}")
                                val matchedRecipes = filterRecipesByInventory(
                                    recipes = allRecipes,
                                    inventoryItems = inventoryItems,
                                    dietaryPreferences = currentProfile?.dietaryPreferences ?: emptyList()
                                )
                                android.util.Log.d("CurrentInventoryScreen", "Matched ${matchedRecipes.size} recipes")
                                val recipesForDisplay = matchedRecipes.map { catalogItem ->
                                    // Use the Firestore document ID directly
                                    mapRecipeCatalogToRecipe(catalogItem, inventoryItems = inventoryItems)
                                }
                                
                                // Save generated meals to user profile
                                if (currentProfile != null) {
                                    val serializedMeals = serializeRecipes(recipesForDisplay)
                                    val mergedGroceryList = buildMergedGroceryListForRecipes(
                                        existingGroceryList = currentProfile.groceryList,
                                        plannedRecipeIds = matchedRecipes.map { it.id }.toSet(),
                                        recipeCatalog = allRecipes,
                                        inventoryItems = inventoryItems,
                                        foodCatalogItems = foodCatalogItems
                                    )
                                    val updatedProfile = currentProfile.copy(
                                        generatedMeals = serializedMeals,
                                        groceryList = mergedGroceryList
                                    )
                                    try {
                                        setUserProfile(updatedProfile)
                                        android.util.Log.d("CurrentInventoryScreen", "Saved ${recipesForDisplay.size} generated meals to profile")
                                    } catch (e: Exception) {
                                        android.util.Log.e("CurrentInventoryScreen", "Failed to save generated meals", e)
                                    }
                                }
                                
                                onNavigateToMeals(recipesForDisplay)
                            } catch (e: Exception) {
                                android.util.Log.e("CurrentInventoryScreen", "Error fetching recipes", e)
                                if (e.message?.contains("PERMISSION_DENIED") == true) {
                                    android.util.Log.e("CurrentInventoryScreen", "PERMISSION_DENIED: Update Firestore security rules to allow read access to 'recipeCatalog' collection")
                                }
                                // Navigate with empty list on error
                                onNavigateToMeals(emptyList())
                            }
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Generate Meals",
                            color = SavrColors.Dark2,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(SavrColors.Sage),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "→",
                            color = SavrColors.White,
                            fontSize = 20.sp,
                            modifier = Modifier.wrapContentSize(Alignment.Center)
                        )
                    }
                }
            }

            // Display all categories grouped by database category
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
                
                items.forEach { group ->
                    val isExpanded = expandedGroups[group.key] == true
                    CurrentInventoryGroupRow(
                        group = group,
                        isExpanded = isExpanded,
                        onToggleExpanded = {
                            expandedGroups[group.key] = !isExpanded
                        }
                    )

                    if (isExpanded) {
                        group.batches.forEach { batch ->
                            CurrentInventoryBatchRow(
                                item = batch.item,
                                onDelete = {
                                    val currentProfile = profile
                                    if (currentProfile != null) {
                                        val updatedList = currentProfile.currentInventory.toMutableList()
                                        updatedList.removeAt(batch.index)
                                        val updatedProfile = currentProfile.copy(currentInventory = updatedList)
                                        scope.launch {
                                            try {
                                                setUserProfile(updatedProfile)
                                            } catch (e: Exception) {
                                                android.util.Log.e("CurrentInventoryScreen", "Failed to delete inventory item", e)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
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
private fun CurrentInventoryGroupRow(
    group: InventoryGroup,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SavrColors.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 7.dp)
            .clickable { onToggleExpanded() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text(group.emoji, fontSize = 22.sp, modifier = Modifier.width(30.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, color = SavrColors.Dark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "${group.totalQuantityLabel} total • ${group.batches.size} ${if (group.batches.size == 1) "batch" else "batches"}",
                    color = SavrColors.TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                ExpiryBadge(label = group.earliestExpiryLabel, status = group.earliestStatus)
                Text(
                    text = if (isExpanded) "Hide" else "Show",
                    color = SavrColors.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CurrentInventoryBatchRow(item: CurrentInventoryItem, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SavrColors.CreamMid,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 56.dp, end = 20.dp, bottom = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.quantity,
                    color = SavrColors.Dark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Separate expiry batch",
                    color = SavrColors.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }

            ExpiryBadge(label = item.expiryLabel, status = item.status)
        }
    }
}
