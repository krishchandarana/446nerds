package com.example.savr.data

import com.savr.app.ui.DayChip
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Generates weekDays for the current week (Monday - Sunday)
 * Returns a list of DayChip and the index of today
 */
fun getCurrentWeekDays(): Pair<List<DayChip>, Int> {
    val today = LocalDate.now()
    val currentDayOfWeek = today.dayOfWeek
    
    // Find Monday of current week
    // If today is Sunday, we need to go back 6 days to get to Monday
    // If today is Monday, daysFromMonday = 0
    val daysFromMonday = when (currentDayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
        else -> 0
    }
    
    val monday = today.minusDays(daysFromMonday.toLong())
    
    // Generate days from Monday (index 0) to Sunday (index 6)
    val weekDays = (0..6).map { dayOffset ->
        val date = monday.plusDays(dayOffset.toLong())
        val dayName = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
            else -> "Mon"
        }
        DayChip(dayName, date.dayOfMonth)
    }
    
    return Pair(weekDays, daysFromMonday)
}

/**
 * Gets the month name for a given date
 */
fun getMonthName(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("MMM")
    return date.format(formatter).uppercase()
}

/**
 * Returns a stable week key for the current week using the Monday date (ISO-8601).
 * Example: "2026-02-23"
 */
fun getCurrentWeekKey(): String {
    val today = LocalDate.now()
    val daysFromMonday = when (today.dayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
        else -> 0
    }
    val monday = today.minusDays(daysFromMonday.toLong())
    return monday.toString()
}

/**
 * Serializes plannedMealsByDay (Map<Int, Set<String>>) to a list of maps for Firestore
 * Format: List of maps with "dayIndex" and "recipeIds" fields
 * Filters out days with no recipes to keep the data clean
 */
fun serializePlannedMeals(plannedMeals: Map<Int, Set<String>>): List<Map<String, Any>> {
    android.util.Log.d("PlanUtils", "Serializing ${plannedMeals.size} days of planned meals")
    val filtered = plannedMeals.filter { (_, recipeIds) -> recipeIds.isNotEmpty() }
    android.util.Log.d("PlanUtils", "After filtering empty days: ${filtered.size} days")
    return filtered.map { (dayIndex, recipeIds) ->
        android.util.Log.d("PlanUtils", "Day $dayIndex: ${recipeIds.size} recipes - $recipeIds")
        mapOf(
            "dayIndex" to dayIndex,
            "recipeIds" to recipeIds.toList()
        )
    }
}

/**
 * Deserializes plannedMeals from Firestore format back to Map<Int, Set<String>>
 */
fun deserializePlannedMeals(serialized: List<Map<String, Any>>): Map<Int, Set<String>> {
    android.util.Log.d("PlanUtils", "Deserializing ${serialized.size} days of planned meals")
    return serialized.associate { map ->
        val dayIndex = (map["dayIndex"] as? Number)?.toInt() ?: 0
        val recipeIds = (map["recipeIds"] as? List<*>)?.mapNotNull { 
            it as? String
        }?.toSet() ?: emptySet()
        android.util.Log.d("PlanUtils", "Day $dayIndex: ${recipeIds.size} recipes - $recipeIds")
        dayIndex to recipeIds
    }
}
