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
    val id: Int,
    val emoji: String,
    val name: String,
    val calories: Int,
    val minutes: Int,
    val matchBadge: String,
    val badgeColor: Color,
    val gradientStart: Color,
    val gradientEnd: Color,
    val isSelected: Boolean = false
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

val CurrentInventoryItems = listOf(
    CurrentInventoryItem(1,  "🥬", "Spinach",         "1 bag",    "1 day",  ExpiryStatus.URGENT,  CurrentInventoryCategory.VEG),
    CurrentInventoryItem(2,  "🍅", "Cherry Tomatoes",  "2 cups",   "5 days", ExpiryStatus.WARNING, CurrentInventoryCategory.VEG),
    CurrentInventoryItem(3,  "🥕", "Carrots",          "4 medium", "12 days",ExpiryStatus.FRESH,   CurrentInventoryCategory.VEG),
    CurrentInventoryItem(4,  "🧄", "Garlic",           "1 bulb",   "18 days",ExpiryStatus.FRESH,   CurrentInventoryCategory.VEG),
    CurrentInventoryItem(5,  "🧀", "Feta Cheese",      "150g",     "2 days", ExpiryStatus.URGENT,  CurrentInventoryCategory.DAIRY),
    CurrentInventoryItem(6,  "🥛", "Whole Milk",       "500ml",    "4 days", ExpiryStatus.WARNING, CurrentInventoryCategory.DAIRY),
    CurrentInventoryItem(7,  "🧈", "Butter",           "200g",     "21 days",ExpiryStatus.FRESH,   CurrentInventoryCategory.DAIRY),
    CurrentInventoryItem(8,  "🥚", "Eggs",             "6 eggs",   "2 days", ExpiryStatus.URGENT,  CurrentInventoryCategory.PROTEIN),
    CurrentInventoryItem(9,  "🍗", "Chicken Breast",   "400g",     "3 days", ExpiryStatus.WARNING, CurrentInventoryCategory.PROTEIN),
    CurrentInventoryItem(10, "🍋", "Lemons",           "3 lemons", "10 days",ExpiryStatus.FRESH,   CurrentInventoryCategory.FRUIT),
    CurrentInventoryItem(11, "🫐", "Blueberries",      "1 punnet", "6 days", ExpiryStatus.WARNING, CurrentInventoryCategory.FRUIT),
    CurrentInventoryItem(12, "🍚", "Basmati Rice",     "500g",     "90 days",ExpiryStatus.FRESH,   CurrentInventoryCategory.GRAIN)
)

val allRecipes = listOf(
    Recipe(
        id = 1, emoji = "🥗", name = "Spinach & Feta Frittata",
        calories = 320, minutes = 25,
        matchBadge = "⚡ 3 expiring used", badgeColor = SavrColors.Terra,
        gradientStart = Color(0xFFFAE9DF), gradientEnd = Color(0x72C4622D),
        isSelected = true
    ),
    Recipe(
        id = 2, emoji = "🍝", name = "Creamed Spinach Pasta",
        calories = 480, minutes = 30,
        matchBadge = "✓ 2 matches", badgeColor = SavrColors.Sage,
        gradientStart = Color(0xFFEBF3EC), gradientEnd = Color(0x667A9E7E),
        isSelected = true
    ),
    Recipe(
        id = 3, emoji = "🍳", name = "Greek Omelette",
        calories = 260, minutes = 15,
        matchBadge = "⚡ 2 expiring used", badgeColor = SavrColors.Terra,
        gradientStart = Color(0xFFFEF3D9), gradientEnd = Color(0x66D4860B)
    ),
    Recipe(
        id = 4, emoji = "🍅", name = "Tomato Basil Shakshuka",
        calories = 310, minutes = 35,
        matchBadge = "✓ 1 match", badgeColor = SavrColors.Sage,
        gradientStart = Color(0xFFFAE9DF), gradientEnd = Color(0x4DC4622D)
    ),
    Recipe(
        id = 5, emoji = "🍗", name = "Roast Chicken & Veg",
        calories = 540, minutes = 50,
        matchBadge = "✓ 2 matches", badgeColor = SavrColors.Sage,
        gradientStart = Color(0xFFEBF3EC), gradientEnd = Color(0x597A9E7E)
    ),
    Recipe(
        id = 6, emoji = "🧀", name = "Feta Stuffed Peppers",
        calories = 290, minutes = 40,
        matchBadge = "⚡ 1 expiring used", badgeColor = SavrColors.Terra,
        gradientStart = Color(0xFFF4EEF8), gradientEnd = Color(0x408250B4)
    ),
    Recipe(
        id = 7, emoji = "🥗", name = "Lemon Garlic Chicken",
        calories = 410, minutes = 35,
        matchBadge = "✓ 1 match", badgeColor = SavrColors.Sage,
        gradientStart = Color(0xFFEAF0FA), gradientEnd = Color(0x333C64C8)
    )
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

val groceryCategories = listOf(
    GroceryCategory("Vegetables", "🥬", listOf(
        GroceryItem(1,  "🧅", "Onion",           "2 large",  isChecked = true),
        GroceryItem(2,  "🫑", "Red Pepper",       "2 medium", isChecked = true),
        GroceryItem(3,  "🌿", "Fresh Basil",      "1 bunch"),
        GroceryItem(4,  "🥦", "Broccoli",         "1 head")
    )),
    GroceryCategory("Dairy", "🧀", listOf(
        GroceryItem(5,  "🧀", "Parmesan",         "100g"),
        GroceryItem(6,  "🫙", "Double Cream",     "250ml",    isChecked = true)
    )),
    GroceryCategory("Protein", "🍗", listOf(
        GroceryItem(11, "🍗", "Chicken Thighs",   "600g"),
        GroceryItem(12, "🥓", "Pancetta",         "150g")
    )),
    GroceryCategory("Other", "🥄", listOf(
        GroceryItem(8,  "🫒", "Olive Oil",        "1 bottle", isChecked = true),
        GroceryItem(9,  "🧂", "Sea Salt",         "1 pack"),
    ))
)
