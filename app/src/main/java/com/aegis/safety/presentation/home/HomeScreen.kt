package com.aegis.safety.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.core.constants.AegisConstants
import com.aegis.safety.presentation.theme.AegisDanger
import com.aegis.safety.presentation.theme.AegisSafe
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    onSos: () -> Unit,
    onContacts: () -> Unit,
    onSafeZones: () -> Unit,
    onTimer: () -> Unit,
    onLiveLocation: () -> Unit,
    onIncidents: () -> Unit,
    onNotifications: () -> Unit,
    onProfile: () -> Unit,
    onPrivacy: () -> Unit,
    onDemo: () -> Unit,
    onMonitoring: () -> Unit,
    onChat: () -> Unit,
    vm: HomeViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onIncidents,
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("Incidents") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onNotifications,
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                    label = { Text("Alerts") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onProfile,
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "Good evening, ${s.greetingName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "YOU ARE SAFE",
                        style = MaterialTheme.typography.headlineMedium,
                        color = AegisSafe,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onPrivacy) {
                    Icon(Icons.Default.Lock, contentDescription = "Privacy")
                }
            }

            Spacer(Modifier.height(20.dp))
            LocationCard(s)
            Spacer(Modifier.height(24.dp))
            SosButton(onSos)
            Spacer(Modifier.height(24.dp))
            QuickActions(
                onContacts = onContacts,
                onSafeZones = onSafeZones,
                onTimer = onTimer,
                onLiveLocation = onLiveLocation,
                onDemo = onDemo,
                onMonitoring = onMonitoring,
                onChat = onChat
            )
        }
    }
}

@Composable
private fun LocationCard(s: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            val loc = s.location
            if (loc != null) {
                Text(
                    "Lat ${"%.4f".format(loc.latitude)}, Lon ${"%.4f".format(loc.longitude)}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Accuracy: ±${loc.accuracyMeters.toInt()} m",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    "Location unavailable — tap Home to retry",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoChip("GPS", s.gpsStatus)
                InfoChip("Net", s.networkStatus)
                InfoChip("Monitor", if (s.monitoringStatus) "ON" else "OFF")
                if (s.pendingCount > 0) InfoChip("Queue", "${s.pendingCount}")
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SosButton(onSos: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var progress by remember { mutableFloatStateOf(0f) }
    var pressed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) {
        if (pressed) {
            val steps = 30
            val stepMs = AegisConstants.SOS_HOLD_DURATION_MS / steps
            for (i in 1..steps) {
                delay(stepMs)
                progress = i.toFloat() / steps
                if (i % 6 == 0) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            if (progress >= 1f) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onSos()
                pressed = false
                progress = 0f
            }
        } else {
            progress = 0f
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        label = "sosScale"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(CircleShape)
            .background(AegisDanger.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .background(AegisDanger)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            tryAwaitRelease()
                            pressed = false
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    if (pressed) "HOLD..." else "HOLD FOR SOS",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (pressed) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.width(120.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    onContacts: () -> Unit,
    onSafeZones: () -> Unit,
    onTimer: () -> Unit,
    onLiveLocation: () -> Unit,
    onDemo: () -> Unit,
    onMonitoring: () -> Unit,
    onChat: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                "Share Location",
                Icons.Default.ShareLocation,
                onLiveLocation,
                Modifier.weight(1f)
            )
            ActionCard(
                "Safety Timer",
                Icons.Default.Timer,
                onTimer,
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                "Contacts",
                Icons.Default.Contacts,
                onContacts,
                Modifier.weight(1f)
            )
            ActionCard(
                "Safe Zones",
                Icons.Default.Security,
                onSafeZones,
                Modifier.weight(1f)
            )
        }
        // New row: auto-detection + AI assistant
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                "Auto Detect",
                Icons.Default.Radar,
                onMonitoring,
                Modifier.weight(1f)
            )
            ActionCard(
                "AI Assistant",
                Icons.Default.AutoAwesome,
                onChat,
                Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                "Demo",
                Icons.Default.Science,
                onDemo,
                Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}