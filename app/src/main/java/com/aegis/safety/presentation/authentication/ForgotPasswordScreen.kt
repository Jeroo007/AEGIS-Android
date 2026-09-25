package com.aegis.safety.presentation.authentication

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Reset password", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        if (!sent) {
            Text("Enter your account email. We'll send a secure reset link.")
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = email, onValueChange = { email = it },
                label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Button(onClick = { sent = true }, enabled = email.contains("@"),
                modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Send Reset Link") }
        } else {
            Text("If an account exists for $email, a reset link has been sent.")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("Back to Sign In") }
    }
}