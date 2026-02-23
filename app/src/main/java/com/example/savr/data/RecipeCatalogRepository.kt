package com.example.savr.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val RECIPE_CATALOG_COLLECTION = "recipeCatalog"

private val firestore: FirebaseFirestore
    get() = FirebaseFirestore.getInstance()

/**
 * Data class for recipe ingredients in the recipeCatalog
 */
data class RecipeIngredient(
    val foodId: String = "",
    val quantity: Int = 0,
    val unit: String = ""
)

/**
 * Data class for recipes in the recipeCatalog collection
 */
data class RecipeCatalogItem(
    val id: String = "", // Firestore document ID (e.g., "banana_oatmeal")
    val calories: Int = 0,
    val description: String = "",
    val dietaryFlags: Map<String, Boolean> = emptyMap(),
    val dietaryRestrictions: List<String> = emptyList(),
    val difficulty: Int = 1,
    val ingredients: List<RecipeIngredient> = emptyList(),
    val prepTimeMinutes: Int = 0,
    val title: String = "",
    val emoji: String = ""
)

/**
 * Fetches all recipes from the recipeCatalog collection
 */
suspend fun getAllRecipes(): List<RecipeCatalogItem> {
    return try {
        Log.d("RecipeCatalogRepo", "Fetching from collection: $RECIPE_CATALOG_COLLECTION")
        val snapshot = firestore.collection(RECIPE_CATALOG_COLLECTION).get().await()
        Log.d("RecipeCatalogRepo", "Fetched ${snapshot.documents.size} recipes")
        
        snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data
                val docId = doc.id // Store the document ID (e.g., "banana_oatmeal")
                Log.d("RecipeCatalogRepo", "Recipe $docId: $data")
                
                // Parse ingredients
                val ingredientsList = mutableListOf<RecipeIngredient>()
                val ingredientsData = data?.get("ingredients")
                if (ingredientsData is List<*>) {
                    ingredientsData.forEach { ingredientItem ->
                        if (ingredientItem is Map<*, *>) {
                            val ingredientMap = ingredientItem as? Map<String, Any> ?: emptyMap()
                            val ingredient = RecipeIngredient(
                                foodId = ingredientMap["foodId"] as? String ?: "",
                                quantity = (ingredientMap["quantity"] as? Number)?.toInt() ?: 0,
                                unit = ingredientMap["unit"] as? String ?: ""
                            )
                            if (ingredient.foodId.isNotBlank()) {
                                ingredientsList.add(ingredient)
                            }
                        }
                    }
                }
                
                // Parse dietary flags (e.g., lactoseFree, peanutFree, vegetarian)
                val dietaryFlags = mutableMapOf<String, Boolean>()
                val dietaryFlagsData = data?.get("dietaryFlags")
                if (dietaryFlagsData is Map<*, *>) {
                    dietaryFlagsData.forEach { (key, value) ->
                        val flagName = key?.toString() ?: ""
                        val flagValue = value as? Boolean ?: false
                        if (flagName.isNotBlank()) {
                            dietaryFlags[flagName] = flagValue
                        }
                    }
                }
                
                // Parse dietary restrictions (array of strings like "Vegetarian")
                val dietaryRestrictions = mutableListOf<String>()
                val dietaryRestrictionsData = data?.get("dietaryRestrictions")
                if (dietaryRestrictionsData is List<*>) {
                    dietaryRestrictionsData.forEach { item ->
                        if (item is String) {
                            dietaryRestrictions.add(item)
                        }
                    }
                }
                
                val recipe = RecipeCatalogItem(
                    id = docId, // Store Firestore document ID (e.g., "peanut_butter_toast")
                    calories = (data?.get("calories") as? Number)?.toInt() ?: 0,
                    description = data?.get("description") as? String ?: "",
                    dietaryFlags = dietaryFlags, // Map<String, Boolean> with keys like "lactoseFree", "peanutFree", "vegetarian"
                    dietaryRestrictions = dietaryRestrictions, // List<String> like ["Vegetarian"]
                    difficulty = (data?.get("difficulty") as? Number)?.toInt() ?: 1,
                    ingredients = ingredientsList, // List<RecipeIngredient> with foodId, quantity, unit
                    prepTimeMinutes = (data?.get("prepTimeMinutes") as? Number)?.toInt() ?: 0,
                    title = data?.get("title") as? String ?: "",
                    emoji = data?.get("emoji") as? String ?: ""
                )
                
                if (recipe.title.isNotBlank()) {
                    Log.d("RecipeCatalogRepo", "Parsed recipe: ${recipe.title} (id: $docId)")
                    recipe
                } else {
                    Log.w("RecipeCatalogRepo", "Skipping recipe $docId - no title")
                    null
                }
            } catch (e: Exception) {
                Log.e("RecipeCatalogRepo", "Error parsing recipe ${doc.id}", e)
                null
            }
        }
    } catch (e: Exception) {
        Log.e("RecipeCatalogRepo", "Error fetching recipes", e)
        if (e.message?.contains("PERMISSION_DENIED") == true) {
            Log.e("RecipeCatalogRepo", "PERMISSION_DENIED: Update Firestore security rules to allow read access to 'recipeCatalog' collection")
            Log.e("RecipeCatalogRepo", "Add this rule: match /recipeCatalog/{document=**} { allow read: if request.auth != null; }")
        }
        e.printStackTrace()
        emptyList()
    }
}
