package com.example.savr.data

import androidx.compose.ui.graphics.Color
import com.savr.app.ui.RecipeIngredientDetail
import com.savr.app.ui.Recipe
import com.savr.app.ui.theme.SavrColors
import org.json.JSONArray
import org.json.JSONObject

/**
 * Serializes a Recipe to a string format for storage in Firestore
 * Uses JSON to preserve complete recipe metadata for UI details.
 */
fun serializeRecipe(recipe: Recipe): String {
    val ingredientsJson = JSONArray()
    recipe.ingredients.forEach { ingredient ->
        ingredientsJson.put(
            JSONObject()
                .put("foodId", ingredient.foodId)
                .put("quantity", ingredient.quantity)
                .put("unit", ingredient.unit)
        )
    }

    val dietaryFlagsJson = JSONObject()
    recipe.dietaryFlags.forEach { (key, value) ->
        dietaryFlagsJson.put(key, value)
    }

    val dietaryRestrictionsJson = JSONArray()
    recipe.dietaryRestrictions.forEach { restriction ->
        dietaryRestrictionsJson.put(restriction)
    }

    return JSONObject()
        .put("id", recipe.id)
        .put("emoji", recipe.emoji)
        .put("name", recipe.name)
        .put("calories", recipe.calories)
        .put("minutes", recipe.minutes)
        .put("description", recipe.description)
        .put("difficulty", recipe.difficulty)
        .put("dietaryFlags", dietaryFlagsJson)
        .put("dietaryRestrictions", dietaryRestrictionsJson)
        .put("ingredients", ingredientsJson)
        .put("matchBadge", recipe.matchBadge)
        .put("isSelected", recipe.isSelected)
        .toString()
}

/**
 * Deserializes a Recipe from a string
 * Supports both new JSON format and legacy pipe-delimited format.
 */
fun deserializeRecipe(serialized: String): Recipe? {
    return try {
        if (serialized.trim().startsWith("{")) {
            val json = JSONObject(serialized)

            val ingredients = mutableListOf<RecipeIngredientDetail>()
            val ingredientsArray = json.optJSONArray("ingredients") ?: JSONArray()
            for (i in 0 until ingredientsArray.length()) {
                val ingredientJson = ingredientsArray.optJSONObject(i) ?: continue
                ingredients.add(
                    RecipeIngredientDetail(
                        foodId = ingredientJson.optString("foodId", ""),
                        quantity = ingredientJson.optInt("quantity", 0),
                        unit = ingredientJson.optString("unit", "")
                    )
                )
            }

            val dietaryFlags = mutableMapOf<String, Boolean>()
            val dietaryFlagsJson = json.optJSONObject("dietaryFlags") ?: JSONObject()
            dietaryFlagsJson.keys().forEach { key ->
                dietaryFlags[key] = dietaryFlagsJson.optBoolean(key, false)
            }

            val dietaryRestrictions = mutableListOf<String>()
            val restrictionsArray = json.optJSONArray("dietaryRestrictions") ?: JSONArray()
            for (i in 0 until restrictionsArray.length()) {
                dietaryRestrictions.add(restrictionsArray.optString(i, ""))
            }

            Recipe(
                id = json.optString("id", ""),
                emoji = json.optString("emoji", ""),
                name = json.optString("name", ""),
                calories = json.optInt("calories", 0),
                minutes = json.optInt("minutes", 0),
                description = json.optString("description", ""),
                difficulty = json.optInt("difficulty", 1),
                dietaryFlags = dietaryFlags,
                dietaryRestrictions = dietaryRestrictions.filter { it.isNotBlank() },
                ingredients = ingredients,
                matchBadge = json.optString("matchBadge", ""),
                badgeColor = SavrColors.Sage,
                gradientStart = Color(0xFFEBF3EC),
                gradientEnd = Color(0x667A9E7E),
                isSelected = json.optBoolean("isSelected", false)
            )
        } else {
            val parts = serialized.split("|")
            if (parts.size >= 6) {
                Recipe(
                    id = parts[0], // String ID (e.g., "banana_oatmeal")
                    emoji = parts[1],
                    name = parts[2],
                    calories = parts[3].toIntOrNull() ?: 0,
                    minutes = parts[4].toIntOrNull() ?: 0,
                    matchBadge = parts[5],
                    badgeColor = SavrColors.Sage,
                    gradientStart = Color(0xFFEBF3EC),
                    gradientEnd = Color(0x667A9E7E),
                    isSelected = false
                )
            } else {
                null
            }
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
