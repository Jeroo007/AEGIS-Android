package com.aegis.safety.presentation.sos

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.presentation.theme.AegisDanger

@Composable
fun SosCountdownScreen(
    onFinished: () -> Unit,
    vm: SosViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) { vm.startCountdown() }

    LaunchedEffect(s.phase) {
        when (s.phase) {
            is SosPhase.Countdown -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            SosPhase.Cancelled -> onFinished()
            else -> Unit
        }
    }

    when (val phase = s.phase) {
        is SosPhase.Countdown -> CountdownView(phase.remaining, onCancel = {
            vm.cancel()
        })
        SosPhase.Sending -> SendingView()
        is SosPhase.Active -> ActiveEmergencyContent(
            incident = phase.incident,
            state = s,
            onResolve = { vm.cancel(); onFinished() }
        )
        is SosPhase.Failed -> FailedView(phase.reason, onClose = onFinished)
        SosPhase.Cancelled -> { /* handled by LaunchedEffect above */ }
    }
}

@Composable
private fun CountdownView(remaining: Int, onCancel: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(AegisDanger.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        val scale by animateFloatAsState(
            targetValue = 1f + (1f - remaining / 5f) * 0.3f,
            animationSpec = tween(800),
            label = "sosPulse"
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Emergency alert in...", color = Color.White,
                style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.size(180.dp).scale(scale)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$remaining", color = Color.White,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, contentColor = AegisDanger),
                modifier = Modifier.height(56.dp).width(200.dp)
            ) { Text("CANCEL", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SendingView() {
    Box(
        Modifier.fillMaxSize().background(AegisDanger.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(Modifier.height(16.dp))
            Text("Sending emergency alert...", color = Color.White)
        }
    }
}

@Composable
private fun FailedView(reason: String, onClose: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(AegisDanger.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Failed to send", color = Color.White,
                style = MaterialTheme.typography.headlineSmall)
            Text(reason, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onClose) { Text("Close") }
        }
    }
}