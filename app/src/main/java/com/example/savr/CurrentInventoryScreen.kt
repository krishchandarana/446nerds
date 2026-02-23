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
import com.example.savr.data.deserializeInventoryItem
import com.example.savr.data.deserializeRecipes
import com.example.savr.data.filterRecipesByInventory
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
            onSave = { serializedItem ->
                val currentProfile = profile
                if (currentProfile != null) {
                    val updatedList = currentProfile.currentInventory + serializedItem
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
                Triple(category, item, index)
            } else {
                null
            }
        }.groupBy { it.first }
    }

    // Create a map that includes all required categories, even if empty
    val allCategoriesWithItems = remember(itemsByCategory) {
        val result = mutableMapOf<String, List<Triple<String, CurrentInventoryItem, Int>>>()
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
                                // Extract all inventory items from the grouped categories
                                val inventoryItems = allCategoriesWithItems.values
                                    .flatten()
                                    .map { it.second } // Extract CurrentInventoryItem from Triple
                                android.util.Log.d("CurrentInventoryScreen", "Inventory items: ${inventoryItems.map { it.name }}")
                                val matchedRecipes = filterRecipesByInventory(allRecipes, inventoryItems)
                                android.util.Log.d("CurrentInventoryScreen", "Matched ${matchedRecipes.size} recipes")
                                val recipesForDisplay = matchedRecipes.map { catalogItem ->
                                    // Use the Firestore document ID directly
                                    mapRecipeCatalogToRecipe(catalogItem, inventoryItems = inventoryItems)
                                }
                                
                                // Save generated meals to user profile
                                val currentProfile = profile
                                if (currentProfile != null) {
                                    val serializedMeals = serializeRecipes(recipesForDisplay)
                                    val updatedProfile = currentProfile.copy(generatedMeals = serializedMeals)
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
                        Text("→", color = SavrColors.White, fontSize = 20.sp)
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
                
                items.forEach { (_, item, index) ->
                    CurrentInventoryItemRow(
                        item = item,
                        onDelete = {
                            val currentProfile = profile
                            if (currentProfile != null) {
                                val updatedList = currentProfile.currentInventory.toMutableList()
                                updatedList.removeAt(index)
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
fun CurrentInventoryItemRow(item: CurrentInventoryItem, onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SavrColors.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
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
            
            Text(item.emoji, fontSize = 22.sp, modifier = Modifier.width(30.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name,     color = SavrColors.Dark,     fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(item.quantity, color = SavrColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
            }
            ExpiryBadge(label = item.expiryLabel, status = item.status)
        }
    }
}
