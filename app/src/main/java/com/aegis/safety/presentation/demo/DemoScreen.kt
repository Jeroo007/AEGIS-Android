package com.aegis.safety.presentation.demo

import androidx.compose.foundation.layout.*
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
fun DemoScreen(
    onBack: () -> Unit,
    vm: DemoViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { AegisTopBar(title = "Demo Mode", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Simulation only", fontWeight = FontWeight.Bold)
                    Text("These buttons do not trigger real emergencies. " +
                        "Events created here are flagged as DEMO.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(16.dp))

            Button(onClick = vm::simulateSos, enabled = !s.running,
                modifier = Modifier.fillMaxWidth()) { Text("SIMULATE SOS") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = vm::simulateAudio, enabled = !s.running,
                modifier = Modifier.fillMaxWidth()) { Text("SIMULATE AUDIO EVENT") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = vm::simulateFall, enabled = !s.running,
                modifier = Modifier.fillMaxWidth()) { Text("SIMULATE FALL") }
            Spacer(Modifier.height(8.dp))
            Button(onClick = vm::simulateMultimodal, enabled = !s.running,
                modifier = Modifier.fillMaxWidth()) { Text("SIMULATE MULTIMODAL EVENT") }

            Spacer(Modifier.height(24.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Pipeline status", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Stage: ${s.lastStage}")
                    Text("Fused score: ${"%.2f".format(s.lastFusedScore)}")
                    Text("Level: ${s.lastLevel}")
                    s.lastIncidentId?.let { Text("Incident: ${it.take(8)}") }
                    if (s.running) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}