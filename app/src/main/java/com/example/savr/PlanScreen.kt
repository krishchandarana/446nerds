package com.savr.app.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.savr.data.getCurrentWeekDays
import com.example.savr.data.getMonthName
import com.savr.app.ui.*
import com.savr.app.ui.components.*
import com.savr.app.ui.theme.SavrColors
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun PlanScreen(
    recipes: List<Recipe>,
    plannedMealsByDay: Map<Int, Set<String>>,
    activeDayIndex: Int,
    currentDayIndex: Int,
    onDaySelected: (Int) -> Unit,
    onNavigateToMeals: () -> Unit,
    onNavigateToGrocery: () -> Unit
) {
    val (weekDays, _) = remember { getCurrentWeekDays() }
    val dayListState = rememberLazyListState()
    val today = LocalDate.now()
    val currentDayOfWeek = today.dayOfWeek
    val daysFromMonday = when (currentDayOfWeek) {
        DayOfWeek.MONDAY -> 0
        DayOfWeek.TUESDAY -> 1
        DayOfWeek.WEDNESDAY -> 2
        DayOfWeek.THURSDAY -> 3
        DayOfWeek.FRIDAY -> 4
        DayOfWeek.SATURDAY -> 5
        DayOfWeek.SUNDAY -> 6
        else -> 0
    }
    val monday = today.minusDays(daysFromMonday.toLong())
    val sunday = monday.plusDays(6)
    val weekRange = "${getMonthName(monday)} ${monday.dayOfMonth} – ${getMonthName(sunday)} ${sunday.dayOfMonth}"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SavrColors.Cream)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        PageHeader(title = "Meal Plan", subtitle = "Week of $weekRange")

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

        LaunchedEffect(currentDayIndex, weekDays.size) {
            if (weekDays.isNotEmpty()) {
                dayListState.scrollToItem(currentDayIndex.coerceIn(0, weekDays.lastIndex))
            }
        }

        LazyRow(
            state = dayListState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(weekDays) { index, day ->
                DayChipItem(
                    day = day,
                    isSelected = activeDayIndex == index,
                    isToday = index == currentDayIndex,
                    onClick = { onDaySelected(index) }
                )
            }
        }

        val dayName = weekDays[activeDayIndex].dayName.uppercase()
        val dayNum = weekDays[activeDayIndex].dayNum
        val activeDate = monday.plusDays(activeDayIndex.toLong())
        val monthName = getMonthName(activeDate)
        Text(
            text       = "$dayName, $monthName $dayNum",
            color      = SavrColors.TextMid,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            modifier   = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )

        val selectedRecipeIds = plannedMealsByDay[activeDayIndex] ?: emptySet()
        val selectedRecipes = recipes.filter { it.id in selectedRecipeIds }

        selectedRecipes.forEach {
            FilledMealSlot(recipe = it, onChangeClick = onNavigateToMeals)
        }

        EmptyMealSlot(onClick = onNavigateToMeals)
    }
}

@Composable
private fun DayChipItem(day: DayChip, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val bg = when {
        isSelected -> SavrColors.Dark
        isToday -> SavrColors.SageTint
        else -> SavrColors.White
    }
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
