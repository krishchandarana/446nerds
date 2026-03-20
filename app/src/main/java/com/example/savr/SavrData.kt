package com.savr.app.ui

import androidx.compose.ui.graphics.Color
import com.savr.app.ui.theme.SavrColors

enum class ExpiryStatus { URGENT, WARNING, FRESH }
enum class CurrentInventoryCategory(val label: String, val emoji: String) {
    ALL("All", ""),
    VEG("Vegetables", "🥦"),
    DAIRY("Dairy", "🥛"),
    PROTEIN("Protein", "🥩"),
    FRUIT("Fruit", "🍎"),
    GRAIN("Grains", "🌾"),
    OTHER("Other", "🥄")
}

enum class NavTab(val label: String, val emoji: String) {
    CURRENTINVENTORY("Inventory", "🧺"),
    MEALS("Meals", "🍽"),
    PLAN("Plan", "📅"),
    GROCERY("Grocery", "🛒"),
    PROFILE("Profile", "👤")
}

data class CurrentInventoryItem(
    val id: Int,
    val emoji: String,
    val name: String,
    val quantity: String,
    val expiryLabel: String,
    val status: ExpiryStatus,
    val category: CurrentInventoryCategory
)

data class Recipe(
    val id: String,
    val emoji: String,
    val name: String,
    val calories: Int,
    val minutes: Int,
    val description: String = "",
    val difficulty: Int = 1,
    val dietaryFlags: Map<String, Boolean> = emptyMap(),
    val dietaryRestrictions: List<String> = emptyList(),
    val ingredients: List<RecipeIngredientDetail> = emptyList(),
    val matchBadge: String,
    val badgeColor: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val isSelected: Boolean = false
)

data class RecipeIngredientDetail(
    val foodId: String,
    val quantity: Int,
    val unit: String
)

data class DayChip(
    val dayName: String,
    val dayNum: Int
)

data class GroceryItem(
    val id: Int,
    val emoji: String,
    val name: String,
    val quantity: String,
    val isChecked: Boolean = false
)

data class GroceryCategory(
    val title: String,
    val emoji: String,
    val items: List<GroceryItem>
)

val weekDays = listOf(
    DayChip("Mon", 17),
    DayChip("Tue", 18),
    DayChip("Wed", 19),
    DayChip("Thu", 20),
    DayChip("Fri", 21),
    DayChip("Sat", 22),
    DayChip("Sun", 23)
)

