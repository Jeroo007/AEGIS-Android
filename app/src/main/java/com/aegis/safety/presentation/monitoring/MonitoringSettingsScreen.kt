package com.aegis.safety.presentation.monitoring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.R
import com.aegis.safety.ai.monitoring.MonitoringConfig
import com.aegis.safety.presentation.components.AegisTopBar

@Composable
fun MonitoringSettingsScreen(
    onBack: () -> Unit,
    vm: MonitoringViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AegisTopBar(
                title = stringResource(R.string.monitoring_settings_title),
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {

            // -----------------------------------------------------------------
            // Master toggle
            // -----------------------------------------------------------------
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.monitoring_enable_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            stringResource(R.string.monitoring_enable_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = s.enabled,
                        onCheckedChange = vm::setEnabled
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // -----------------------------------------------------------------
            // Signals
            // -----------------------------------------------------------------
            Text(
                "Signals",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Choose which signals AEGIS monitors locally.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            SignalRow(
                label = stringResource(R.string.monitoring_signal_audio),
                checked = s.audio,
                enabled = s.enabled,
                onChange = vm::setAudio
            )
            SignalRow(
                label = stringResource(R.string.monitoring_signal_motion),
                checked = s.motion,
                enabled = s.enabled,
                onChange = vm::setMotion
            )
            SignalRow(
                label = stringResource(R.string.monitoring_signal_vision),
                checked = s.vision,
                enabled = s.enabled,
                onChange = vm::setVision
            )

            Spacer(Modifier.height(24.dp))

            // -----------------------------------------------------------------
            // Sensitivity
            // -----------------------------------------------------------------
            Text(
                stringResource(R.string.monitoring_sensitivity_label),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                sensitivityDescription(s.sensitivity),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                MonitoringConfig.Sensitivity.entries.forEach { level ->
                    FilterChip(
                        selected = s.sensitivity == level,
                        enabled = s.enabled,
                        onClick = { vm.setSensitivity(level) },
                        label = { Text(level.name) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // -----------------------------------------------------------------
            // Confirmation window
            // -----------------------------------------------------------------
            Text(
                "${stringResource(R.string.monitoring_confirmation_label)}: ${s.confirmationSeconds}s",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.monitoring_confirmation_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = s.confirmationSeconds.toFloat(),
                onValueChange = { vm.setConfirmationSeconds(it.toInt()) },
                valueRange = 5f..60f,
                steps = 10,
                enabled = s.enabled
            )

            Spacer(Modifier.height(24.dp))

            // -----------------------------------------------------------------
            // Activity counter
            // -----------------------------------------------------------------
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Escalations in the last hour",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${s.escalationsLastHour} of ${12}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // -----------------------------------------------------------------
            // Safety notice
            // -----------------------------------------------------------------
            Text(
                stringResource(R.string.monitoring_safety_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// -----------------------------------------------------------------------------
// Subcomponents
// -----------------------------------------------------------------------------

@Composable
private fun SignalRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onChange
        )
    }
}

private fun sensitivityDescription(level: MonitoringConfig.Sensitivity): String = when (level) {
    MonitoringConfig.Sensitivity.LOW ->
        "Fewer alerts, more missed events. Best if you rarely need automatic detection."
    MonitoringConfig.Sensitivity.MEDIUM ->
        "Balanced. Recommended for everyday use."
    MonitoringConfig.Sensitivity.HIGH ->
        "More alerts, more false prompts. Best if you're in higher-risk situations."
}