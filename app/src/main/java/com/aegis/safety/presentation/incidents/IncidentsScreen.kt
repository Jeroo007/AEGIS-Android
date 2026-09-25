package com.aegis.safety.presentation.incidents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.domain.models.Incident
import com.aegis.safety.presentation.components.AegisTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncidentsScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    vm: IncidentsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { AegisTopBar(title = "Incident History", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding)) {
            if (s.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (s.incidents.isEmpty() && !s.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No incidents recorded")
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.incidents, key = { it.id }) { inc ->
                        IncidentCard(inc, onClick = { onOpen(inc.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentCard(inc: Incident, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(inc.type.name.replace("_", " "),
                        fontWeight = FontWeight.SemiBold)
                    Text(inc.status.name.replace("_", " "),
                        style = MaterialTheme.typography.bodySmall)
                }
                if (inc.isDemo) {
                    AssistChip(onClick = {}, label = { Text("DEMO") })
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(inc.createdAt)),
                style = MaterialTheme.typography.bodySmall)
            inc.latitude?.let { lat ->
                Text("${"%.4f".format(lat)}, ${"%.4f".format(inc.longitude ?: 0.0)}",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}