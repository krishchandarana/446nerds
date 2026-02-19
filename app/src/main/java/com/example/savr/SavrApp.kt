package com.savr.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.savr.app.ui.components.SavrBottomNav
import com.savr.app.ui.screens.CurrentInventory.CurrentInventoryScreen
import com.savr.app.ui.screens.grocery.GroceryScreen
import com.savr.app.ui.screens.meals.MealsScreen
import com.savr.app.ui.screens.plan.PlanScreen
import com.savr.app.ui.screens.profile.ProfileScreen

@Composable
fun SavrApp(modifier: Modifier = Modifier) {
    var currentTab by remember { mutableStateOf(NavTab.PLAN) }

    Column(modifier = modifier) {

        Box(modifier = Modifier.weight(1f)) {
            Crossfade(
                targetState   = currentTab,
                animationSpec = tween(durationMillis = 180),
                label         = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    NavTab.CURRENTINVENTORY  -> CurrentInventoryScreen(
                        onNavigateToMeals = { currentTab = NavTab.MEALS }
                    )
                    NavTab.MEALS -> MealsScreen(
                        onNavigateToPlan = { currentTab = NavTab.PLAN }
                    )
                    NavTab.PLAN    -> PlanScreen(
                        onNavigateToMeals   = { currentTab = NavTab.MEALS },
                        onNavigateToGrocery = { currentTab = NavTab.GROCERY }
                    )
                    NavTab.GROCERY -> GroceryScreen()
                    NavTab.PROFILE -> ProfileScreen()
                }
            }
        }
        SavrBottomNav(
            currentTab    = currentTab,
            onTabSelected = { currentTab = it }
        )
    }
}
