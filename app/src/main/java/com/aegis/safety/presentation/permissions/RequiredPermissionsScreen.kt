package com.aegis.safety.presentation.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private data class RequiredPermission(
    val permission: String,
    val label: String,
    val why: String,
    val icon: ImageVector
)

@Composable
fun RequiredPermissionsScreen(
    onContinue: () -> Unit
) {
    val permissions = remember {
        buildList {
            add(RequiredPermission(
                Manifest.permission.ACCESS_FINE_LOCATION,
                "Precise location",
                "To send your location during an emergency and find nearby safe zones",
                Icons.Default.LocationOn
            ))
            add(RequiredPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                "Approximate location",
                "Fallback if precise location is unavailable",
                Icons.Default.MyLocation
            ))
            add(RequiredPermission(
                Manifest.permission.SEND_SMS,
                "Send SMS",
                "To automatically text your emergency contacts your location during SOS",
                Icons.AutoMirrored.Filled.Message
            ))
            add(RequiredPermission(
                Manifest.permission.RECORD_AUDIO,
                "Microphone",
                "Optional — only for on-device distress detection",
                Icons.Default.Mic
            ))
            add(RequiredPermission(
                Manifest.permission.CAMERA,
                "Camera",
                "Optional — only for on-device fall/motion detection",
                Icons.Default.Videocam
            ))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(RequiredPermission(
                    Manifest.permission.POST_NOTIFICATIONS,
                    "Notifications",
                    "To alert you about SOS status and safety timers",
                    Icons.Default.Notifications
                ))
            }
        }
    }

    var requested by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onContinue()
    }

    // Auto-launch on first composition
    LaunchedEffect(Unit) {
        if (!requested) {
            requested = true
            launcher.launch(permissions.map { it.permission }.toTypedArray())
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Icon(
                Icons.Default.Shield,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Permissions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "AEGIS needs a few permissions to protect you. " +
                    "Microphone and camera are optional — you can enable them later.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(20.dp))

            permissions.forEach { p ->
                PermissionRow(p)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    launcher.launch(permissions.map { it.permission }.toTypedArray())
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Grant permissions")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue anyway")
            }
            Text(
                "Some features will not work without these permissions. " +
                    "You can change them anytime in Settings → Permissions.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun PermissionRow(p: RequiredPermission) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(p.icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.label, fontWeight = FontWeight.SemiBold)
                Text(
                    p.why,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}