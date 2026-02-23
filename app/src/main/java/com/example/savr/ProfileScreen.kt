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
import com.example.savr.data.deleteUserDocumentWithCallbacks
import com.example.savr.data.setUserProfile
import com.example.savr.data.userProfileFlow
import com.google.firebase.auth.FirebaseAuth
import com.savr.app.ui.components.TagChip
import com.savr.app.ui.components.TagStyle
import com.savr.app.ui.theme.SavrColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val DIETARY_PREFERENCE_OPTIONS = listOf(
    "Lactose Free",
    "Vegetarian",
    "Peanut Free"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    val scope = rememberCoroutineScope()

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
    
    // State for dropdown
    var expanded by remember { mutableStateOf(false) }
    var selectedPreferences by remember { mutableStateOf(emptySet<String>()) }
    
    // Update selected preferences when profile changes
    LaunchedEffect(profile) {
        selectedPreferences = (profile?.dietaryPreferences ?: emptyList()).toSet()
    }

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
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Multi-select dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedPreferences.isEmpty()) "Select dietary preferences" else "${selectedPreferences.size} selected",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedTextColor = SavrColors.Dark,
                            unfocusedTextColor = SavrColors.Dark,
                            focusedBorderColor = SavrColors.Sage,
                            unfocusedBorderColor = SavrColors.TextMuted
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(SavrColors.White)
                    ) {
                        DIETARY_PREFERENCE_OPTIONS.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = option,
                                            color = SavrColors.Dark,
                                            fontSize = 14.sp
                                        )
                                        Checkbox(
                                            checked = selectedPreferences.contains(option),
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = SavrColors.Sage,
                                                uncheckedColor = SavrColors.TextMuted
                                            )
                                        )
                                    }
                                },
                                onClick = {
                                    val newSelectedPreferences = if (selectedPreferences.contains(option)) {
                                        selectedPreferences - option
                                    } else {
                                        selectedPreferences + option
                                    }
                                    selectedPreferences = newSelectedPreferences
                                    
                                    // Save preferences when selection changes
                                    val currentProfile = profile
                                    val updatedProfile = if (currentProfile != null) {
                                        currentProfile.copy(dietaryPreferences = newSelectedPreferences.toList())
                                    } else {
                                        // If profile doesn't exist yet, create one with current auth data
                                        UserProfile(
                                            displayName = displayName,
                                            username = authUser?.email ?: "",
                                            dietaryPreferences = newSelectedPreferences.toList()
                                        )
                                    }
                                    scope.launch {
                                        try {
                                            setUserProfile(updatedProfile)
                                        } catch (e: Exception) {
                                            android.util.Log.e("ProfileScreen", "Failed to save dietary preferences", e)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
                
                // Display selected preferences as chips
                if (selectedPreferences.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Selected Preferences",
                            color = SavrColors.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            selectedPreferences.sorted().forEach { pref ->
                                TagChip(pref, TagStyle.SAGE)
                            }
                        }
                    }
                }
            }
        }

        SectionCard(title = "ACCOUNT") {
            SettingsLinkRow("🚪", "Sign Out", onClick = {
                FirebaseAuth.getInstance().signOut()
            })
            HorizontalDivider(color = SavrColors.DividerColour, modifier = Modifier.padding(horizontal = 16.dp))
            SettingsLinkRow("🗑️", "Delete Account", textColor = SavrColors.Terra, onClick = {
                val auth = FirebaseAuth.getInstance()
                val user = auth.currentUser
                if (user != null) {
                    val uid = user.uid
                    // First delete the Firestore document
                    deleteUserDocumentWithCallbacks(
                        uid = uid,
                        onSuccess = {
                            // Then delete the Firebase Auth user
                            user.delete()
                                .addOnSuccessListener {
                                    // Sign out after successful deletion
                                    auth.signOut()
                                }
                                .addOnFailureListener { e ->
                                    android.util.Log.e("ProfileScreen", "Failed to delete Auth user", e)
                                    // Still sign out even if Auth deletion fails
                                    auth.signOut()
                                }
                        },
                        onFailure = { e ->
                            android.util.Log.e("ProfileScreen", "Failed to delete Firestore document", e)
                        }
                    )
                }
            })
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

