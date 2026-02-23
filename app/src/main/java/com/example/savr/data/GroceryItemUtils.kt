package com.example.savr.data

import com.savr.app.ui.CurrentInventoryItem
import com.savr.app.ui.CurrentInventoryCategory
import com.savr.app.ui.ExpiryStatus
import com.savr.app.ui.GroceryItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Calculates ExpiryStatus based on the expiry date string (DD/MM/YYYY format)
 * URGENT: <= 2 days remaining
 * WARNING: 3-7 days remaining
 * FRESH: > 7 days remaining
 */
fun calculateExpiryStatus(expiryDateString: String): ExpiryStatus {
    return try {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val expiryDate = LocalDate.parse(expiryDateString.trim(), formatter)
        val today = LocalDate.now()
        val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate)
        
        when {
            daysUntilExpiry <= 2 -> ExpiryStatus.URGENT
            daysUntilExpiry <= 7 -> ExpiryStatus.WARNING
            else -> ExpiryStatus.FRESH
        }
    } catch (e: Exception) {
        // If date parsing fails, default to FRESH
        ExpiryStatus.FRESH
    }
}

/**
 * Serializes a GroceryItem to a string format for storage in user profile
 * Format: "emoji|name|category|quantity"
 */
fun serializeGroceryItem(item: GroceryItem, category: String): String {
    return "${item.emoji}|${item.name}|$category|${item.quantity}"
}

/**
 * Deserializes a string from user profile to a GroceryItem
 * Format: "emoji|name|category|quantity"
 */
fun deserializeGroceryItem(serialized: String, id: Int, isChecked: Boolean = false): GroceryItem? {
    return try {
        val parts = serialized.split("|")
        if (parts.size >= 4) {
            GroceryItem(
                id = id,
                emoji = parts[0],
                name = parts[1],
                quantity = parts[3], // quantity already includes the unit
                isChecked = isChecked
            )
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Extracts category from serialized grocery item string
 */
fun getCategoryFromSerialized(serialized: String): String? {
    return try {
        val parts = serialized.split("|")
        if (parts.size >= 3) parts[2] else null
    } catch (e: Exception) {
        null
    }
}

/**
 * Serializes a CurrentInventoryItem to a string format for storage in user profile
 * Format: "emoji|name|category|quantity|expiryDate|status"
 * category is the string from the database (foodCatalog), not the enum name
 */
fun serializeInventoryItem(item: CurrentInventoryItem, category: String): String {
    return "${item.emoji}|${item.name}|$category|${item.quantity}|${item.expiryLabel}|${item.status.name}"
}

/**
 * Deserializes a string from user profile to a CurrentInventoryItem
 * Format: "emoji|name|category|quantity|expiryDate|status"
 * Recalculates expiry status based on current date
 */
fun deserializeInventoryItem(serialized: String, id: Int): CurrentInventoryItem? {
    return try {
        val parts = serialized.split("|")
        if (parts.size >= 6) {
            // Map database category string to CurrentInventoryCategory enum
            val categoryString = parts[2]
            val category = when (categoryString.lowercase()) {
                "fruit", "fruits" -> CurrentInventoryCategory.FRUIT
                "vegetables", "vegetable", "produce" -> CurrentInventoryCategory.VEG
                "dairy", "dairy & eggs", "eggs" -> CurrentInventoryCategory.DAIRY
                "protein", "meat" -> CurrentInventoryCategory.PROTEIN
                "grain", "grains", "pantry", "baking" -> CurrentInventoryCategory.GRAIN
                else -> CurrentInventoryCategory.OTHER
            }
            // Recalculate status based on current date
            val expiryDateString = parts[4]
            val status = calculateExpiryStatus(expiryDateString)
            CurrentInventoryItem(
                id = id,
                emoji = parts[0],
                name = parts[1],
                quantity = parts[3],
                expiryLabel = expiryDateString,
                status = status,
                category = category
            )
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Extracts category string from serialized inventory item string (from database)
 */
fun getCategoryFromInventorySerialized(serialized: String): String? {
    return try {
        val parts = serialized.split("|")
        if (parts.size >= 3) parts[2] else null
    } catch (e: Exception) {
        null
    }
}
