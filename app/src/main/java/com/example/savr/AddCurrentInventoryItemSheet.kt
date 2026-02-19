package com.savr.app.ui.screens.CurrentInventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savr.app.ui.CurrentInventoryCategory
import com.savr.app.ui.CurrentInventoryItem
import com.savr.app.ui.ExpiryStatus
import com.savr.app.ui.theme.SavrColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCurrentInventoryItemSheet(
    onSave:    (CurrentInventoryItem) -> Unit,
    onDismiss: () -> Unit
) {
    var name          by remember { mutableStateOf("") }
    var quantity      by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }
    var selectedCat   by remember { mutableStateOf<CurrentInventoryCategory?>(null) }
    val canSave = name.isNotBlank() && quantity.isNotBlank() && selectedCat != null
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss,
        containerColor   = SavrColors.White,
        shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
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
            SheetSectionLabel(
                text     = "Item name",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(6.dp))
            SheetTextField(
                value         = name,
                onValueChange = { name = it },
                placeholder   = "e.g. Spinach, Feta, Eggs…",
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(14.dp))
            SheetSectionLabel(
                text     = "Quantity",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(6.dp))
            SheetTextField(
                value         = quantity,
                onValueChange = { quantity = it },
                placeholder   = "e.g. 1 bag, 200g, 6 eggs…",
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(14.dp))
            SheetSectionLabel(
                text     = "Category",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(8.dp))
            val cats = CurrentInventoryCategory.values()
                .filter { it != CurrentInventoryCategory.ALL }

            Column(
                modifier              = Modifier.padding(horizontal = 20.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                cats.chunked(3).forEach { rowCats ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCats.forEach { cat ->
                            val isSelected = cat == selectedCat
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) SavrColors.SageTint else SavrColors.Cream
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isSelected) SavrColors.Sage
                                                else Color(0xFFE2DDD5),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCat = cat }
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(cat.emoji, fontSize = 20.sp)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    text       = cat.label,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color      = if (isSelected) SavrColors.Sage
                                                 else SavrColors.TextMid
                                )
                            }
                        }
                        repeat(3 - rowCats.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            SheetSectionLabel(
                text     = "Best-by date (MM/DD/YYYY)",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(6.dp))
            SheetTextField(
                value         = expirationDate,
                onValueChange = { expirationDate = it },
                placeholder   = "DD/MM/YYYY",
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (canSave) SavrColors.Dark else SavrColors.CreamMid)
                    .clickable(enabled = canSave) {
                        val newItem = CurrentInventoryItem(
                            id          = System.currentTimeMillis().toInt(), // temp id
                            emoji       = selectedCat!!.emoji,
                            name        = name.trim(),
                            quantity    = quantity.trim(),
                            expiryLabel = expirationDate,
                            status      = ExpiryStatus.FRESH,
                            category    = selectedCat!!
                        )
                        onSave(newItem)

                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = "+ Add to Inventory",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color      = if (canSave) SavrColors.White else SavrColors.TextMuted
                )
            }
        }
    }
}
@Composable
private fun SheetSectionLabel(text: String, modifier: Modifier = Modifier) {
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
        placeholder   = {
            Text(placeholder, color = SavrColors.TextMuted, fontSize = 15.sp)
        },
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
