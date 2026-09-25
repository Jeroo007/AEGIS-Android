package com.aegis.safety.presentation.authentication

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.R

@Composable
fun LoginScreen(
    onNavigateRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    onSuccess: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.success) { if (s.success) onSuccess() }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Image(
            painter = painterResource(R.drawable.ic_aegis_logo),
            contentDescription = "AEGIS Logo",
            modifier = Modifier.height(72.dp).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(20.dp))
        Text("Welcome back", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Sign in to AEGIS", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = s.email, onValueChange = vm::onEmail,
            label = { Text("Email") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = s.password, onValueChange = vm::onPassword,
            label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth())

        s.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = vm::login, enabled = s.canSubmitLogin,
            modifier = Modifier.fillMaxWidth().height(56.dp)) {
            if (s.loading) CircularProgressIndicator(
                modifier = Modifier.size(22.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary)
            else Text("Sign In")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onForgotPassword) { Text("Forgot password?") }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account?")
            TextButton(onClick = onNavigateRegister) { Text("Register") }
        }
    }
}