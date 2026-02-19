package com.savr.app.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

//TODO: Fix Generate grocery list positioning
@Composable
fun PlanScreen(onNavigateToMeals: () -> Unit, onNavigateToGrocery: () -> Unit) {
    var activeDayIndex by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SavrColors.Cream)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        PageHeader(title = "Meal Plan", subtitle = "Week of Feb 17 – Feb 23")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SavrColors.Sage)
                        .clickable { onNavigateToGrocery() }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = "Generate Grocery List",
                        color      = SavrColors.White,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            weekDays.forEachIndexed { index, day ->
                DayChipItem(
                    day      = day,
                    isActive = activeDayIndex == index,
                    onClick  = { activeDayIndex = index }
                )
            }
        }

        Text(
            text       = "TUESDAY, FEB 18",
            color      = SavrColors.TextMid,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier   = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )

        tuesdayMeals.forEach { slot ->
            FilledMealSlot(slot = slot, onChangeClick = onNavigateToMeals)
        }

        EmptyMealSlot(label = "Add Dinner", onClick = onNavigateToMeals)
        EmptyMealSlot(label = "Add Snack",  onClick = onNavigateToMeals)

    }
}


@Composable
private fun DayChipItem(day: DayChip, isActive: Boolean, onClick: () -> Unit) {
    val bg          = if (isActive) SavrColors.Dark else SavrColors.White
    val textColor   = if (isActive) SavrColors.White else SavrColors.Dark
    val mutedColor  = if (isActive) SavrColors.White.copy(alpha = 0.6f) else SavrColors.TextMuted
    val dotColor    = if (isActive) SavrColors.SageLight else SavrColors.Sage
    val borderColor = when {
        isActive   -> SavrColors.Dark
        else        -> SavrColors.White
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .defaultMinSize(minWidth = 42.dp)
    ) {
        Text(day.dayName, color = mutedColor,  fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Text(day.dayNum.toString(), color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        if (day.hasMeal) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

@Composable
private fun FilledMealSlot(slot: MealSlot, onChangeClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SavrColors.White,
        shadowElevation = 1.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 9.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text       = slot.slotLabel,
                    color      = SavrColors.TextMuted,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text       = "Change",
                    color      = SavrColors.Sage,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable { onChangeClick() }
                )
            }

            HorizontalDivider(color = SavrColors.DividerColour)

            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Text(slot.emoji, fontSize = 26.sp, modifier = Modifier.width(36.dp))
                Column {
                    Text(
                        text  = slot.mealName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = SavrColors.Dark,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TagChip("${slot.calories} cal", TagStyle.GRAY)
                        TagChip("${slot.minutes} min",  TagStyle.GRAY)

                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMealSlot(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 9.dp)
            .border(2.dp, SavrColors.DividerColour, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("+", color = SavrColors.TextMuted, fontSize = 16.sp)
            Text(label, color = SavrColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

