package com.savr.app.ui.screens.grocery

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.savr.data.FoodCatalogItem
import com.example.savr.data.getAllFoodCatalogItems
import com.example.savr.data.serializeGroceryItem
import com.savr.app.ui.CurrentInventoryCategory
import com.savr.app.ui.GroceryItem
import com.savr.app.ui.theme.SavrColors
import kotlinx.coroutines.launch
enum class GroceryCategory(val label: String, val emoji: String) {
    Vegetables(    "Vegetables",      "🥦"),
    Fruit(     "Fruits",       "🍎"),
    DAIRY(      "Dairy", "🥛"),
    Protein(     "Protein",       "🥩"),
    Grain(       "Grain",  "🌾"),
    OTHER(      "Other",        "🥄")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroceryItemSheet(
    onSave:    (serializedItem: String) -> Unit,
    onDismiss: () -> Unit
) {
    var foodCatalogItems by remember { mutableStateOf<List<FoodCatalogItem>>(emptyList()) }
    var isLoadingItems by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf<FoodCatalogItem?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            isLoadingItems = true
            try {
                val items = getAllFoodCatalogItems()
                android.util.Log.d("AddGroceryItemSheet", "Loaded ${items.size} items from foodCatalog")
                foodCatalogItems = items
            } catch (e: Exception) {
                android.util.Log.e("AddGroceryItemSheet", "Error loading food catalog items", e)
            } finally {
                isLoadingItems = false
            }
        }
    }
    
    val canSave = selectedItem != null && quantity.isNotBlank() && quantity.toIntOrNull() != null && quantity.toInt() > 0
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor   = SavrColors.White,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDDDDDD))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text       = "Add Item",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = SavrColors.Dark
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(SavrColors.CreamMid)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 13.sp, color = SavrColors.TextMid)
                }
            }
            SheetLabel(
                text     = "Select Item",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(6.dp))
            
            // Dropdown for food catalog items
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                OutlinedTextField(
                    value = selectedItem?.let { "${it.emoji} ${it.name}" } ?: "Select an item",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                        focusedTextColor = SavrColors.Dark,
                        unfocusedTextColor = SavrColors.Dark,
                        focusedBorderColor = SavrColors.Sage,
                        unfocusedBorderColor = Color(0xFFE2DDD5)
                    ),
                    shape = RoundedCornerShape(13.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(SavrColors.White)
                ) {
                    if (isLoadingItems) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Loading items...",
                                    color = SavrColors.TextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {}
                        )
                    } else if (foodCatalogItems.isEmpty()) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "No items found",
                                    color = SavrColors.TextMuted,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {}
                        )
                    } else {
                        foodCatalogItems.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(item.emoji, fontSize = 18.sp)
                                        Text(
                                            text = item.name,
                                            color = SavrColors.Dark,
                                            fontSize = 14.sp
                                        )
                                    }
                                },
                                onClick = {
                                    selectedItem = item
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SheetLabel(
                text     = "Quantity",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(6.dp))
            
            // Numerical quantity input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decrease button
                Box(
                                modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SavrColors.CreamMid)
                        .clickable {
                            val current = quantity.toIntOrNull() ?: 1
                            if (current > 1) {
                                quantity = (current - 1).toString()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", fontSize = 20.sp, color = SavrColors.Dark, fontWeight = FontWeight.Bold)
                }
                
                // Quantity display
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || (newValue.toIntOrNull() != null && newValue.toInt() > 0)) {
                            quantity = newValue
                        }
                    },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SavrColors.Sage,
                        unfocusedBorderColor = Color(0xFFE2DDD5),
                        focusedContainerColor = SavrColors.White,
                        unfocusedContainerColor = SavrColors.Cream,
                        cursorColor = SavrColors.Sage
                    ),
                    shape = RoundedCornerShape(13.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = SavrColors.Dark),
                    singleLine = true
                )
                
                // Unit display
                Text(
                    text = selectedItem?.defaultUnit ?: "",
                    color = SavrColors.TextMuted,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                // Increase button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SavrColors.CreamMid)
                        .clickable {
                            val current = quantity.toIntOrNull() ?: 1
                            quantity = (current + 1).toString()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontSize = 20.sp, color = SavrColors.Dark, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (canSave) SavrColors.Dark else SavrColors.CreamMid)
                    .clickable(enabled = canSave) {
                        val item = selectedItem!!
                        val qty = quantity.toInt()
                        val quantityWithUnit = "$qty ${item.defaultUnit}"
                        val newItem = GroceryItem(
                            id = System.currentTimeMillis().toInt(),
                            emoji = item.emoji,
                            name = item.name,
                            quantity = quantityWithUnit
                        )
                        val serialized = serializeGroceryItem(newItem, item.category)
                        onSave(serialized)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "+ Add to List",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (canSave) SavrColors.White else SavrColors.TextMuted
                )
            }
        }
    }
}
@Composable
private fun SheetLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text          = text.uppercase(),
        fontSize      = 10.sp,
        fontWeight    = FontWeight.ExtraBold,
        letterSpacing = 0.8.sp,
        color         = SavrColors.TextMuted,
        modifier      = modifier
    )
}

@Composable
private fun SheetTextField(
    value:         String,
    onValueChange: (String) -> Unit,
    placeholder:   String,
    modifier:      Modifier = Modifier
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = { Text(placeholder, color = SavrColors.TextMuted, fontSize = 15.sp) },
        modifier      = modifier,
        shape         = RoundedCornerShape(13.dp),
        singleLine    = true,
        colors        = OutlinedTextFieldDefaults.colors(
            focusedBorderColor      = SavrColors.Sage,
            unfocusedBorderColor    = Color(0xFFE2DDD5),
            focusedContainerColor   = SavrColors.White,
            unfocusedContainerColor = SavrColors.Cream,
            cursorColor             = SavrColors.Sage
        ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = SavrColors.Dark)
    )
}
