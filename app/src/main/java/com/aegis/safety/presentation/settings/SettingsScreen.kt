package com.aegis.safety.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.core.datastore.UserPreferences
import com.aegis.safety.presentation.components.AegisTopBar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkTheme: String = "system",
    val language: String = "en",
    val demoMode: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences
) : androidx.lifecycle.ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            prefs.darkTheme.collect { v -> _state.update { it.copy(darkTheme = v) } }
        }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            prefs.language.collect { v -> _state.update { it.copy(language = v) } }
        }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            prefs.demoMode.collect { v -> _state.update { it.copy(demoMode = v) } }
        }
    }

    fun setTheme(v: String) = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        .launch { prefs.setDarkTheme(v) }
    fun setLanguage(v: String) = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        .launch { prefs.setLanguage(v) }
    fun setDemo(v: Boolean) = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        .launch { prefs.setDemoMode(v) }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AegisTopBar(title = "Settings", onBack = onBack) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            
            SectionLabel("APPEARANCE")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Theme", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(s.darkTheme.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("light", "dark", "system").forEach { t ->
                            FilterChip(
                                selected = s.darkTheme == t, 
                                onClick = { vm.setTheme(t) },
                                label = { Text(t.replaceFirstChar { it.uppercase() }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            SectionLabel("LANGUAGE")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("en" to "English", "ta" to "Tamil", "hi" to "Hindi").forEach { (k, v) ->
                            FilterChip(
                                selected = s.language == k, 
                                onClick = { vm.setLanguage(k) },
                                label = { Text(v) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            SectionLabel("DEVELOPER")
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), 
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Enable demo mode", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = s.demoMode, 
                        onCheckedChange = vm::setDemo,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, 
        style = MaterialTheme.typography.labelMedium, 
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}