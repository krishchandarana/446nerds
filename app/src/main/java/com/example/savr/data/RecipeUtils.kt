package com.example.savr.data

import com.savr.app.ui.Recipe

/**
 * Serializes a Recipe to a string format for storage in Firestore
 * Format: "id|emoji|name|calories|minutes|matchBadge"
 */
fun serializeRecipe(recipe: Recipe): String {
    return "${recipe.id}|${recipe.emoji}|${recipe.name}|${recipe.calories}|${recipe.minutes}|${recipe.matchBadge}"
}

/**
 * Deserializes a Recipe from a string
 * Note: badgeColor, gradientStart, gradientEnd, isSelected are not stored and will use defaults
 */
fun deserializeRecipe(serialized: String): Recipe? {
    return try {
        val parts = serialized.split("|")
        if (parts.size >= 6) {
            Recipe(
                id = parts[0], // String ID (e.g., "banana_oatmeal")
                emoji = parts[1],
                name = parts[2],
                calories = parts[3].toIntOrNull() ?: 0,
                minutes = parts[4].toIntOrNull() ?: 0,
                matchBadge = parts[5],
                badgeColor = com.savr.app.ui.theme.SavrColors.Sage, // Default color
                gradientStart = androidx.compose.ui.graphics.Color(0xFFEBF3EC), // Default gradient
                gradientEnd = androidx.compose.ui.graphics.Color(0x667A9E7E),
                isSelected = false
            )
        } else {
            null
        }
    } catch (e: Exception) {
        android.util.Log.e("RecipeUtils", "Error deserializing recipe: $serialized", e)
        null
    }
}

/**
 * Serializes a list of Recipes to a list of strings
 */
fun serializeRecipes(recipes: List<Recipe>): List<String> {
    return recipes.map { serializeRecipe(it) }
}

/**
 * Deserializes a list of strings to a list of Recipes
 */
fun deserializeRecipes(serialized: List<String>): List<Recipe> {
    return serialized.mapNotNull { deserializeRecipe(it) }
}
