package com.savr.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.savr.app.ui.NavTab
import com.savr.app.ui.theme.SavrColors

@Composable
fun SavrBottomNav(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        HorizontalDivider(color = SavrColors.Dark.copy(alpha = 0.07f))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SavrColors.White)
                .padding(bottom = 10.dp, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavTab.values().forEach { tab ->
                NavItem(
                    tab       = tab,
                    isActive  = currentTab == tab,
                    onClick   = { onTabSelected(tab) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(tab: NavTab, isActive: Boolean, onClick: () -> Unit) {
    val contentColor = if (isActive) SavrColors.Sage else SavrColors.Dark.copy(alpha = 0.35f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(tab.emoji, fontSize = 20.sp)
        Text(
            text       = tab.label,
            color      = contentColor,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
