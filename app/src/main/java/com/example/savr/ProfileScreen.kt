package com.savr.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.savr.data.UserProfile
import com.example.savr.data.userProfileFlow
import com.google.firebase.auth.FirebaseAuth
import com.savr.app.ui.components.TagChip
import com.savr.app.ui.components.TagStyle
import com.savr.app.ui.theme.SavrColors
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProfileScreen() {
    var profile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        userProfileFlow().collectLatest { profile = it }
    }

    val authUser = FirebaseAuth.getInstance().currentUser
    val displayName = profile?.displayName?.takeIf { it.isNotBlank() }
        ?: authUser?.displayName?.takeIf { it.isNotBlank() }
        ?: "Savr User"
    val username = profile?.username?.let { raw ->
        when {
            raw.contains("@") -> raw  // email stored as username: show as-is
            raw.startsWith("@") -> raw
            else -> "@$raw"
        }
    } ?: authUser?.email?.let { "@${it.substringBefore('@')}" }
        ?: ""
    val dietaryPreferences = profile?.dietaryPreferences ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SavrColors.Cream)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 90.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = SavrColors.Dark2)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Text(
                            text = displayName,
                            color = SavrColors.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (username.isNotBlank()) {
                            Text(
                                text = username,
                                color = SavrColors.SageLight,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        SectionCard(title = "DIETARY PREFERENCES") {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dietaryPreferences.isEmpty()) {
                    Text(
                        text = "None set",
                        color = SavrColors.TextMuted,
                        fontSize = 14.sp
                    )
                } else {
                    dietaryPreferences.forEach { pref ->
                        TagChip(" $pref", TagStyle.SAGE)
                    }
                }
            }
        }

        SectionCard(title = "ACCOUNT") {
            SettingsLinkRow("🚪", "Sign Out", onClick = {
                FirebaseAuth.getInstance().signOut()
            })
            HorizontalDivider(color = SavrColors.DividerColour, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsLinkRow("🗑️", "Delete Account", textColor = SavrColors.Terra)
        }
    }
}



@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            text       = title,
            color      = SavrColors.TextMuted,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier   = Modifier.padding(start = 4.dp, bottom = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SavrColors.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
private fun SettingsLinkRow(
    emoji: String,
    label: String,
    textColor: androidx.compose.ui.graphics.Color = SavrColors.Dark,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(emoji, fontSize = 16.sp)
            Text(label, color = textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Text("›", color = SavrColors.TextMuted, fontSize = 18.sp)
    }
}

