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
import com.example.savr.data.createUserDocumentWithCallbacks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.savr.app.ui.theme.SavrColors

@Composable
fun CreateAccountScreen(
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val auth = remember { FirebaseAuth.getInstance() }

    val passwordsMatch = password == confirmPassword
    val canSubmit = name.isNotBlank() &&
            email.isNotBlank() &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            passwordsMatch &&
            password.length >= 6

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
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "←",
                    fontSize = 24.sp,
                    color = SavrColors.Dark,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(end = 8.dp)
                )
                Text(
                    text = "Create account",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SavrColors.Dark
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display name", color = SavrColors.TextMid) },
                placeholder = { Text("e.g. Aisha", color = SavrColors.TextMuted) },
                singleLine = true,
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
                placeholder = { Text("At least 6 characters", color = SavrColors.TextMuted) },
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

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm password", color = SavrColors.TextMid) },
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

            if (confirmPassword.isNotBlank() && !passwordsMatch) {
                Text(
                    text = "Passwords don't match",
                    color = SavrColors.Terra,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )
            }

            if (password.isNotBlank() && password.length < 6) {
                Text(
                    text = "Password must be at least 6 characters",
                    color = SavrColors.Terra,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                )
            }

            Spacer(Modifier.height(20.dp))

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
                        color = if (canSubmit && !isLoading)
                            SavrColors.Sage else SavrColors.CreamMid
                    )
                    .clickable(enabled = canSubmit && !isLoading) {
                        errorMessage = null
                        isLoading = true
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { result ->
                                val uid = result.user?.uid
                                if (uid == null) {
                                    isLoading = false
                                    errorMessage = "Account created but no user ID. Try signing in."
                                    return@addOnSuccessListener
                                }
                                val displayName = name.trim()
                                createUserDocumentWithCallbacks(
                                    uid = uid,
                                    displayName = displayName,
                                    username = email.trim(),
                                    onSuccess = {
                                        if (displayName.isNotBlank()) {
                                            val profileUpdates = UserProfileChangeRequest.Builder()
                                                .setDisplayName(displayName)
                                                .build()
                                            result.user?.updateProfile(profileUpdates)
                                                ?.addOnCompleteListener {
                                                    isLoading = false
                                                    onSuccess()
                                                }
                                                ?: run {
                                                    isLoading = false
                                                    onSuccess()
                                                }
                                        } else {
                                            isLoading = false
                                            onSuccess()
                                        }
                                    },
                                    onFailure = { e ->
                                        isLoading = false
                                        android.util.Log.e("CreateAccount", "Failed to create Firestore user doc", e)
                                        errorMessage = e.message ?: "Could not save profile."
                                    }
                                )
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = e.message ?: "Could not create account"
                            }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLoading) "Creating account…" else "Create account",
                    color = if (canSubmit && !isLoading)
                        SavrColors.White else SavrColors.TextMuted,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Already have an account? Sign in",
                color = SavrColors.Sage,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onBack() }
            )
        }
    }
}
