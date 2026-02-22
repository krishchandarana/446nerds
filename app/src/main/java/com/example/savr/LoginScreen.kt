package com.savr.app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.savr.app.ui.theme.SavrColors

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToCreateAccount: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val auth = remember { FirebaseAuth.getInstance() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SavrColors.Cream)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Savr",
                style = MaterialTheme.typography.displayMedium,
                color = SavrColors.Dark
            )
            Text(
                text = "Plan meals, reduce waste",
                color = SavrColors.TextMuted,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = SavrColors.TextMid) },
                placeholder = { Text("you@example.com", color = SavrColors.TextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SavrColors.Sage,
                    unfocusedBorderColor = Color(0xFFE2DDD5),
                    focusedContainerColor = SavrColors.White,
                    unfocusedContainerColor = SavrColors.Cream,
                    cursorColor = SavrColors.Sage,
                    focusedLabelColor = SavrColors.Sage,
                    unfocusedLabelColor = SavrColors.TextMid
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = SavrColors.Dark)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = SavrColors.TextMid) },
                placeholder = { Text("••••••••", color = SavrColors.TextMuted) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SavrColors.Sage,
                    unfocusedBorderColor = Color(0xFFE2DDD5),
                    focusedContainerColor = SavrColors.White,
                    unfocusedContainerColor = SavrColors.Cream,
                    cursorColor = SavrColors.Sage,
                    focusedLabelColor = SavrColors.Sage,
                    unfocusedLabelColor = SavrColors.TextMid
                ),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = SavrColors.Dark)
            )

            Spacer(Modifier.height(24.dp))

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = SavrColors.Terra,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        color = if (email.isNotBlank() && password.isNotBlank() && !isLoading)
                            SavrColors.Sage else SavrColors.CreamMid
                    )
                    .clickable(enabled = email.isNotBlank() && password.isNotBlank() && !isLoading) {
                        errorMessage = null
                        isLoading = true
                        auth.signInWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener {
                                isLoading = false
                                onLoginSuccess()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = e.message ?: "Sign in failed"
                            }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLoading) "Signing in…" else "Sign In",
                    color = if (email.isNotBlank() && password.isNotBlank() && !isLoading)
                        SavrColors.White else SavrColors.TextMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Create account",
                color = SavrColors.Sage,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onNavigateToCreateAccount() }
            )
        }
    }
}
