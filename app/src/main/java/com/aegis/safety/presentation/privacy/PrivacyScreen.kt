package com.aegis.safety.presentation.privacy

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.presentation.components.AegisTopBar

@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    vm: PrivacyViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { AegisTopBar(title = "Privacy Center", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Safety Monitoring", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            PrivacyToggle("Master monitoring", s.monitoringEnabled, vm::setMonitoring)
            PrivacyToggle("Location sharing", s.locationSharing, vm::setLocationSharing)
            PrivacyToggle("Microphone monitoring", s.audioMonitoring, vm::setAudioMonitoring)
            PrivacyToggle("Camera monitoring", s.visionMonitoring, vm::setVisionMonitoring)
            PrivacyToggle("Analytics", s.analyticsEnabled, vm::setAnalytics)
            PrivacyToggle("Notifications", s.notificationsEnabled, vm::setNotifications)

            Spacer(Modifier.height(24.dp))
            Text("About your data",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("• Audio and video are processed locally by default.\n" +
                "• Raw audio/video is not uploaded unless you enable it.\n" +
                "• Location data is only shared during an active emergency.\n" +
                "• You can delete your data at any time from Settings.",
                style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PrivacyToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}