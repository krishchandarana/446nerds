package com.savr.app.ui.screens.meals

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savr.app.ui.*
import com.savr.app.ui.components.*
import com.savr.app.ui.theme.SavrColors

@Composable
fun MealsScreen(
    selectedIds: Set<Int>,
    onToggleRecipe: (Int) -> Unit,
    onNavigateToPlan: () -> Unit
) {
    val selectedCount = selectedIds.size
    val countText = when (selectedCount) {
        0    -> "No meals selected"
        1    -> "1 meal selected"
        else -> "$selectedCount meals selected"
    }

    Box(modifier = Modifier.fillMaxSize().background(SavrColors.Cream)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 140.dp)
        ) {
            PageHeader(
                title    = "Select Meals",
                subtitle = "Matched to your fridge · 7 recipes found"
            )

            allRecipes.forEach { recipe ->
                val isSelected = recipe.id in selectedIds
                RecipeCard(
                    recipe     = recipe,
                    isSelected = isSelected,
                    onToggle   = { onToggleRecipe(recipe.id) }
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SavrColors.Dark)
                    .padding(horizontal = 20.dp, vertical = 11.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text       = countText,
                            color      = SavrColors.White,
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text     = "Tap cards to add or remove",
                            color    = SavrColors.White.copy(alpha = 0.45f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SavrColors.Sage)
                            .clickable(enabled = selectedCount > 0) { onNavigateToPlan() }
                            .padding(horizontal = 18.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text       = "Add to Plan →",
                            color      = SavrColors.White,
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeCard(
    recipe: Recipe,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val borderColor = if (isSelected) SavrColors.Amber else SavrColors.White.copy(alpha = 0f)

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SavrColors.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 10.dp)
            .border(
                width = 2.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onToggle() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(
                       color = SavrColors.AmberTint
                    )
            ) {
                Text(
                    text     = recipe.emoji,
                    fontSize = 38.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) SavrColors.Amber
                            else SavrColors.White.copy(alpha = 0.15f)
                        )
                        .then(
                            if (!isSelected) Modifier.border(
                                width = 2.dp,
                                color = SavrColors.White.copy(alpha = 0.8f),
                                shape = CircleShape
                            ) else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "✓",
                        color = if (isSelected) SavrColors.White
                                else SavrColors.White.copy(alpha = 0f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp)) {
                Text(
                    text     = recipe.name,
                    style    = MaterialTheme.typography.headlineSmall,
                    color    = SavrColors.Dark,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MetaPill(" ${recipe.calories} cal")
                    MetaPill(" ${recipe.minutes} min")
                }
            }
        }
    }
}
