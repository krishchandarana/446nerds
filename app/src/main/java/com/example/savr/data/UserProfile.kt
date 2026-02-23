package com.example.savr.data

/**
 * User profile stored in Firestore at users/{uid}.
 * The document ID is the Firebase Auth UID.
 */
data class UserProfile(
    val displayName: String = "",
    val username: String = "",
    val dietaryPreferences: List<String> = emptyList(),
    val groceryList: List<String> = emptyList(),
    val currentInventory: List<String> = emptyList(),
    val generatedMeals: List<String> = emptyList(),
    val plannedMeals: List<Map<String, Any>> = emptyList(),
    val plannedMealsWeekKey: String = ""
)
