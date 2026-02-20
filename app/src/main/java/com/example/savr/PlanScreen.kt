package com.savr.app.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
fun PlanScreen(
    plannedMealsByDay: Map<Int, Set<Int>>,
    activeDayIndex: Int,
    onDaySelected: (Int) -> Unit,
    onNavigateToMeals: () -> Unit,
    onNavigateToGrocery: () -> Unit
) {
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
                .padding(horizontal = 20.dp)
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
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            weekDays.forEachIndexed { index, day ->
                DayChipItem(
                    day = day,
                    isSelected = activeDayIndex == index,
                    onClick = { onDaySelected(index) }
                )
            }
        }

        val dayName = weekDays[activeDayIndex].dayName.uppercase()
        val dayNum = weekDays[activeDayIndex].dayNum
        Text(
            text       = "$dayName, FEB $dayNum",
            color      = SavrColors.TextMid,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier   = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )

        val selectedRecipeIds = plannedMealsByDay[activeDayIndex] ?: emptySet()
        val selectedRecipes = allRecipes.filter { it.id in selectedRecipeIds }

        selectedRecipes.forEach {
            FilledMealSlot(recipe = it, onChangeClick = onNavigateToMeals)
        }

        EmptyMealSlot(onClick = onNavigateToMeals)
    }
}

@Composable
private fun DayChipItem(day: DayChip, isSelected: Boolean, onClick: () -> Unit) {
    val bg = if (isSelected) SavrColors.Dark else SavrColors.White
    val textColor = if (isSelected) SavrColors.White else SavrColors.Dark
    val mutedColor = if (isSelected) SavrColors.White.copy(alpha = 0.6f) else SavrColors.TextMuted

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = 56.dp, height = 60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable { onClick() }
    ) {
        Text(day.dayName, color = mutedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Text(day.dayNum.toString(), color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun FilledMealSlot(recipe: Recipe, onChangeClick: () -> Unit) {
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
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Text(recipe.emoji, fontSize = 26.sp, modifier = Modifier.width(36.dp))
                Column {
                    Text(
                        text  = recipe.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = SavrColors.Dark,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        TagChip("${recipe.calories} cal", TagStyle.GRAY)
                        TagChip("${recipe.minutes} min", TagStyle.GRAY)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMealSlot( onClick: () -> Unit) {
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
            Text("Add Meal", color = SavrColors.TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}
