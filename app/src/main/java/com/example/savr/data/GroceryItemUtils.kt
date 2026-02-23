package com.example.savr.data

import com.savr.app.ui.GroceryItem

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
