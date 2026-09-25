package com.aegis.safety.presentation.help

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.safety.presentation.components.AegisTopBar

@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(topBar = { AegisTopBar(title = "Help", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("How to use AEGIS", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            HelpItem("Hold SOS", "Press and hold the red SOS button for 1.5 seconds. A countdown starts, and after 5 seconds an emergency alert is sent.")
            HelpItem("Safety Timer", "Set a timer. If you don't check in before it expires, we escalate to your contacts.")
            HelpItem("Emergency Contacts", "Add trusted people. They get notified during an SOS.")
            HelpItem("Safe Zones", "See nearby police stations, hospitals, and verified safe places.")
            HelpItem("Demo Mode", "Simulate AI events to see the full pipeline — clearly marked as demo, never mistaken for real incidents.")
        }
    }
}

@Composable
private fun HelpItem(title: String, body: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}