package com.savr.app.ui.screens.CurrentInventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savr.app.ui.*
import com.savr.app.ui.components.*
import com.savr.app.ui.theme.SavrColors

@Composable
fun CurrentInventoryScreen(onNavigateToMeals: () -> Unit) {
    var selectedCategory by remember { mutableStateOf(CurrentInventoryCategory.ALL) }
    var showAddSheet by remember { mutableStateOf(false) }
    val items = remember { mutableStateListOf(*CurrentInventoryItems.toTypedArray()) }

    if (showAddSheet) {
        AddCurrentInventoryItemSheet(
            onSave = { newItem ->
                items.add(newItem)
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(SavrColors.Cream)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp)
        ) {
            PageHeader(title = "My Current Inventory", subtitle = "")

            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(SavrColors.SageTint)
                    .clickable { onNavigateToMeals() }
                    .padding(horizontal = 16.dp, vertical = 15.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Generate Meal Plan",
                            color = SavrColors.Dark2,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 22.sp,
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(SavrColors.Sage),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("→", color = SavrColors.White, fontSize = 20.sp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                CurrentInventoryCategory.values().forEach { cat ->
                    val label = if (cat == CurrentInventoryCategory.ALL) "All"
                                else "${cat.emoji} ${cat.label}"
                    FilterPill(
                        text     = label,
                        isActive = selectedCategory == cat,
                        onClick  = { selectedCategory = cat }
                    )
                }
            }

            if (selectedCategory == CurrentInventoryCategory.ALL) {
                CurrentInventoryCategory.values()
                    .filter { it != CurrentInventoryCategory.ALL }
                    .forEach { cat ->
                        val catItems = items.filter { it.category == cat }
                        if (catItems.isNotEmpty()) {
                            SectionHeader("${cat.emoji} ${cat.label.uppercase()}")
                            catItems.forEach { item -> CurrentInventoryItemRow(item) }
                            Spacer(Modifier.height(4.dp))
                        }
                    }
            } else {
                val filteredItems = items.filter { it.category == selectedCategory }
                filteredItems.forEach { item -> CurrentInventoryItemRow(item) }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 88.dp)
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SavrColors.Dark)
                .clickable { showAddSheet = true },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = SavrColors.White, fontSize = 22.sp)
        }
    }
}

@Composable
fun CurrentInventoryItemRow(item: CurrentInventoryItem) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SavrColors.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 7.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text(item.emoji, fontSize = 22.sp, modifier = Modifier.width(30.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name,     color = SavrColors.Dark,     fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(item.quantity, color = SavrColors.TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 1.dp))
            }
            ExpiryBadge(label = item.expiryLabel, status = item.status)
        }
    }
}
