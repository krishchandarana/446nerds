package com.example.savr.data

import com.savr.app.ui.CurrentInventoryItem
import com.savr.app.ui.CurrentInventoryCategory
import com.savr.app.ui.ExpiryStatus
import com.savr.app.ui.GroceryItem
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

const val GROCERY_SOURCE_USER = "user"
const val GROCERY_SOURCE_RECIPE = "recipe"

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
 * Format: "emoji|name|category|quantity|source"
 */
fun serializeGroceryItem(
    item: GroceryItem,
    category: String,
    source: String = GROCERY_SOURCE_USER
): String {
    return "${item.emoji}|${item.name}|$category|${item.quantity}|$source"
}

/**
 * Deserializes a string from user profile to a GroceryItem
 * Format: "emoji|name|category|quantity|source"
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

fun getGrocerySource(serialized: String): String {
    return try {
        val parts = serialized.split("|")
        when {
            parts.size >= 5 && parts[4].isNotBlank() -> parts[4]
            else -> GROCERY_SOURCE_USER
        }
    } catch (e: Exception) {
        GROCERY_SOURCE_USER
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

private data class InventorySerializedParts(
    val emoji: String,
    val name: String,
    val category: String,
    val quantity: String,
    val expiryDate: String,
    val status: String
)

private fun parseInventorySerializedParts(serialized: String): InventorySerializedParts? {
    return try {
        val parts = serialized.split("|")
        if (parts.size < 6) return null
        InventorySerializedParts(
            emoji = parts[0],
            name = parts[1],
            category = parts[2],
            quantity = parts[3],
            expiryDate = parts[4],
            status = parts[5]
        )
    } catch (e: Exception) {
        null
    }
}

private fun parseQuantity(quantity: String): Pair<Int, String>? {
    val trimmed = quantity.trim()
    val match = Regex("""^(\d+)\s*(.*)$""").find(trimmed) ?: return null
    val amount = match.groupValues[1].toIntOrNull() ?: return null
    val unit = match.groupValues[2].trim()
    return amount to unit
}

private fun parseInventoryDate(date: String): LocalDate? {
    return try {
        LocalDate.parse(date.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (_: DateTimeParseException) {
        null
    }
}

/**
 * Merge duplicate inventory entries so items like eggs are stacked into one row.
 * Duplicates are matched by name + category + quantity unit.
 * Quantities are summed and the earliest expiry date is kept.
 */
fun mergeSerializedInventoryItems(items: List<String>): List<String> {
    val merged = linkedMapOf<String, String>()

    items.forEach { serialized ->
        val candidate = parseInventorySerializedParts(serialized) ?: run {
            merged["raw:${merged.size}:$serialized"] = serialized
            return@forEach
        }

        val quantityParts = parseQuantity(candidate.quantity) ?: run {
            merged["raw:${merged.size}:$serialized"] = serialized
            return@forEach
        }

        val key = "${candidate.name.lowercase()}|${candidate.category.lowercase()}|${quantityParts.second.lowercase()}"
        val existingSerialized = merged[key]
        if (existingSerialized == null) {
            merged[key] = serialized
            return@forEach
        }

        val existing = parseInventorySerializedParts(existingSerialized) ?: run {
            merged[key] = serialized
            return@forEach
        }
        val existingQuantityParts = parseQuantity(existing.quantity) ?: run {
            merged[key] = serialized
            return@forEach
        }

        val totalQuantity = existingQuantityParts.first + quantityParts.first
        val unit = existingQuantityParts.second.ifBlank { quantityParts.second }

        val existingDate = parseInventoryDate(existing.expiryDate)
        val candidateDate = parseInventoryDate(candidate.expiryDate)
        val chosenDate = when {
            existingDate == null -> candidate.expiryDate
            candidateDate == null -> existing.expiryDate
            candidateDate.isBefore(existingDate) -> candidate.expiryDate
            else -> existing.expiryDate
        }

        val mergedItem = CurrentInventoryItem(
            id = 0,
            emoji = existing.emoji,
            name = existing.name,
            quantity = listOf(totalQuantity.toString(), unit).filter { it.isNotBlank() }.joinToString(" "),
            expiryLabel = chosenDate,
            status = calculateExpiryStatus(chosenDate),
            category = when (existing.category.lowercase()) {
                "fruit", "fruits" -> CurrentInventoryCategory.FRUIT
                "vegetables", "vegetable", "produce" -> CurrentInventoryCategory.VEG
                "dairy", "dairy & eggs", "eggs" -> CurrentInventoryCategory.DAIRY
                "protein", "meat" -> CurrentInventoryCategory.PROTEIN
                "grain", "grains", "pantry", "baking" -> CurrentInventoryCategory.GRAIN
                else -> CurrentInventoryCategory.OTHER
            }
        )

        merged[key] = serializeInventoryItem(mergedItem, existing.category)
    }

    return merged.values.toList()
}
