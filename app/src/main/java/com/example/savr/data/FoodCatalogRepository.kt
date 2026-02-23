package com.example.savr.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

private const val FOOD_CATALOG_COLLECTION = "foodCatalog"

private val firestore: FirebaseFirestore
    get() = FirebaseFirestore.getInstance()

/**
 * Data class for items in the foodCatalog collection
 */
data class FoodCatalogItem(
    val category: String = "",
    val defaultUnit: String = "",
    val emoji: String = "",
    val name: String = ""
)

/**
 * Fetches all items from the foodCatalog collection
 */
suspend fun getAllFoodCatalogItems(): List<FoodCatalogItem> {
    return try {
        Log.d("FoodCatalogRepo", "Fetching from collection: $FOOD_CATALOG_COLLECTION")
        val snapshot = firestore.collection(FOOD_CATALOG_COLLECTION).get().await()
        Log.d("FoodCatalogRepo", "Fetched ${snapshot.documents.size} documents")
        
        snapshot.documents.mapNotNull { doc ->
            try {
                val data = doc.data
                Log.d("FoodCatalogRepo", "Document ${doc.id}: $data")
                
                // Manually extract fields to handle any naming issues
                val item = FoodCatalogItem(
                    category = data?.get("category") as? String ?: "",
                    defaultUnit = data?.get("defaultUnit") as? String ?: "",
                    emoji = data?.get("emoji") as? String ?: "",
                    name = data?.get("name") as? String ?: ""
                )
                
                // Only return items that have at least a name
                if (item.name.isNotBlank()) {
                    Log.d("FoodCatalogRepo", "Parsed item: ${item.name}")
                    item
                } else {
                    Log.w("FoodCatalogRepo", "Skipping item ${doc.id} - no name")
                    null
                }
            } catch (e: Exception) {
                Log.e("FoodCatalogRepo", "Error parsing food catalog item ${doc.id}", e)
                null
            }
        }
    } catch (e: Exception) {
        Log.e("FoodCatalogRepo", "Error fetching food catalog", e)
        e.printStackTrace()
        emptyList()
    }
}
