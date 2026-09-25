package com.aegis.safety.presentation.incidents

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.safety.domain.models.Incident
import com.aegis.safety.domain.repository.IncidentRepositoryInterface
import com.aegis.safety.presentation.components.AegisTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel as HiltVM
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltVM
class IncidentDetailsViewModel @Inject constructor(
    private val repo: IncidentRepositoryInterface
) : ViewModel() {
    var incident by mutableStateOf<Incident?>(null)
        private set

    fun load(id: String) {
        viewModelScope.launch {
            incident = repo.observeIncidents().first().firstOrNull { it.id == id }
        }
    }
}

@Composable
fun IncidentDetailsScreen(
    incidentId: String,
    onBack: () -> Unit,
    vm: IncidentDetailsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    LaunchedEffect(incidentId) { vm.load(incidentId) }
    Scaffold(topBar = { AegisTopBar(title = "Incident", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            val inc = vm.incident
            if (inc == null) {
                Text("Loading…")
            } else {
                Text(inc.type.name.replace("_", " "),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Field("Status", inc.status.name.replace("_", " "))
                Field("Severity", inc.severity.name)
                Field("Created", SimpleDateFormat("MMM d, yyyy HH:mm:ss",
                    Locale.getDefault()).format(Date(inc.createdAt)))
                inc.latitude?.let {
                    Field("Latitude", "%.6f".format(it))
                    Field("Longitude", "%.6f".format(inc.longitude ?: 0.0))
                    Field("Accuracy", "±${inc.accuracyMeters?.toInt() ?: 0} m")
                }
                inc.confidence?.let { Field("AI confidence", "%.2f".format(it)) }
                inc.notes?.let { Field("Notes", it) }
                if (inc.isDemo) Field("Mode", "DEMO — not a real incident")
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Spacer(Modifier.height(8.dp))
    Text(label, style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.bodyLarge)
}