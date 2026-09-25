package com.aegis.safety.presentation.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.presentation.components.AegisTopBar

@Composable
fun SafetyTimerScreen(
    onBack: () -> Unit,
    vm: SafetyTimerViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val options = listOf(15, 30, 60, 120)

    Scaffold(topBar = { AegisTopBar(title = "Check On Me", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(20.dp)) {
            if (s.active == null) {
                Text("Set a safety timer. We'll check in on you at the end.",
                    style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(220.dp)
                ) {
                    items(options) { min ->
                        Card(onClick = { vm.start(min) }, modifier = Modifier.height(96.dp)) {
                            Column(Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center) {
                                Text("$min", style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold)
                                Text("minutes", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            } else {
                val active = s.active!!
                val mins = s.remainingSeconds / 60
                val secs = s.remainingSeconds % 60
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Timer, contentDescription = null,
                            modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(String.format("%02d:%02d", mins, secs),
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold)
                        Text(if (s.expired) "TIME'S UP" else "Remaining",
                            color = if (s.expired) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = vm::checkIn) { Text("I'm Safe") }
                            OutlinedButton(onClick = vm::cancel) { Text("Cancel") }
                        }
                    }
                }
                active.label?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("Note: $it", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}