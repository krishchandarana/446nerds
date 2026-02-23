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
import com.example.savr.data.UserProfile
import com.example.savr.data.deserializeGroceryItem
import com.example.savr.data.getCategoryFromSerialized
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
    var checkedItems by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        userProfileFlow().collectLatest { 
            profile = it
            // Load checked items from profile (items ending with |checked)
            val checked = mutableSetOf<Int>()
            it?.groceryList?.forEachIndexed { index, serialized ->
                if (serialized.endsWith("|checked")) {
                    checked.add(index)
                }
            }
            checkedItems = checked
        }
    }

    if (showAddSheet) {
        AddGroceryItemSheet(
            onSave = { serializedItem ->
                val currentProfile = profile
                if (currentProfile != null) {
                    val updatedList = currentProfile.groceryList + serializedItem
                    val updatedProfile = currentProfile.copy(groceryList = updatedList)
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
        val items = profile?.groceryList ?: emptyList()
        items.mapIndexedNotNull { index, serialized ->
            val item = deserializeGroceryItem(serialized.replace("|checked", ""), index, index in checkedItems)
            val category = getCategoryFromSerialized(serialized.replace("|checked", "")) ?: "Other"
            if (item != null) {
                Triple(category, item, index)
            } else {
                null
            }
        }.groupBy { it.first }
    }

    // Create a map that includes all required categories, even if empty
    val allCategoriesWithItems = remember(itemsByCategory) {
        val result = mutableMapOf<String, List<Triple<String, GroceryItem, Int>>>()
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

                items.forEach { (_, item, index) ->
                    val isChecked = index in checkedItems
                    GroceryItemRow(
                        item = item,
                        isChecked = isChecked,
                        onToggle = {
                            val currentProfile = profile
                            if (currentProfile != null) {
                                val updatedChecked = if (isChecked) {
                                    checkedItems - index
                                } else {
                                    checkedItems + index
                                }
                                checkedItems = updatedChecked
                                
                                // Update serialized string to include/remove checked flag
                                val updatedList = currentProfile.groceryList.toMutableList()
                                val serialized = updatedList[index].replace("|checked", "")
                                updatedList[index] = if (updatedChecked.contains(index)) {
                                    "$serialized|checked"
                                } else {
                                    serialized
                                }
                                
                                val updatedProfile = currentProfile.copy(groceryList = updatedList)
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
                                // Remove the item from the list
                                val updatedList = currentProfile.groceryList.toMutableList()
                                updatedList.removeAt(index)
                                
                                // Update checked items indices (shift down indices after deleted item)
                                val updatedChecked = checkedItems.filter { it < index }.toSet() +
                                        checkedItems.filter { it > index }.map { it - 1 }.toSet()
                                checkedItems = updatedChecked
                                
                                val updatedProfile = currentProfile.copy(groceryList = updatedList)
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
private fun GroceryItemRow(item: GroceryItem, isChecked: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
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

            Text(
                text           = item.name,
                color          = if (isChecked) SavrColors.TextMuted else SavrColors.Dark,
                fontSize       = 14.sp,
                fontWeight     = FontWeight.Medium,
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                modifier       = Modifier
                    .weight(1f)
                    .clickable { onToggle() }
            )

            Text(
                text       = item.quantity,
                color      = SavrColors.TextMuted,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium
            )

        }

    }

}
