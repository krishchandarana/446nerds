package com.savr.app.ui.screens.grocery

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savr.app.ui.GroceryItem
import com.savr.app.ui.groceryCategories
import com.savr.app.ui.components.PageHeader
import com.savr.app.ui.theme.SavrColors



// TODO: IMPLEMENT THE ADD BUTTON
//Box(
//modifier = Modifier
//.align(Alignment.BottomEnd)
//.padding(end = 20.dp, bottom = 88.dp)
//.size(50.dp)
//.clip(RoundedCornerShape(16.dp))
//.background(SavrColors.Dark)
//.clickable { /* add item */ },   //TODO
//contentAlignment = Alignment.Center
//) {
//    Text("+", color = SavrColors.White, fontSize = 22.sp)
//}
@Composable
fun GroceryScreen() {
    var checkedIds by remember {
        mutableStateOf(
            groceryCategories.flatMap { it.items }
                .filter { it.isChecked }
                .map { it.id }
                .toSet()
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SavrColors.Cream)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        PageHeader("Grocery List", subtitle = "")

        groceryCategories.forEach { category ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(category.emoji, fontSize = 14.sp)
                Text(
                    text       = category.title.uppercase(),
                    color      = SavrColors.TextMid,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            category.items.forEach { item ->
                val isChecked = item.id in checkedIds
                GroceryItemRow(
                    item      = item,
                    isChecked = isChecked,
                    onToggle  = {
                        checkedIds = if (isChecked) {
                            checkedIds - item.id
                        } else {
                            checkedIds + item.id
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun GroceryItemRow(item: GroceryItem, isChecked: Boolean, onToggle: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SavrColors.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 6.dp)
            .clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isChecked) SavrColors.Sage else SavrColors.CreamMid),
                contentAlignment = Alignment.Center
            ) {
                if (isChecked) {
                    Text("✓", color = SavrColors.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text(item.emoji, fontSize = 18.sp)

            Text(
                text           = item.name,
                color          = if (isChecked) SavrColors.TextMuted else SavrColors.Dark,
                fontSize       = 14.sp,
                fontWeight     = FontWeight.Medium,
                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None,
                modifier       = Modifier.weight(1f)
            )

            Text(
                text       = item.quantity,
                color      = SavrColors.TextMuted,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium
            )

        }

    }

}
