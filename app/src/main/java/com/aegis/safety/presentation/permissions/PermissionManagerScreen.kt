package com.aegis.safety.presentation.permissions

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.safety.presentation.components.AegisTopBar
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionManagerScreen(onBack: () -> Unit) {
    val location = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val mic = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    val cam = rememberPermissionState(Manifest.permission.CAMERA)
    val notif = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    Scaffold(topBar = { AegisTopBar(title = "Permission Manager", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            PermRow("Location", location.status.isGranted) { location.launchPermissionRequest() }
            PermRow("Microphone", mic.status.isGranted) { mic.launchPermissionRequest() }
            PermRow("Camera", cam.status.isGranted) { cam.launchPermissionRequest() }
            PermRow("Notifications", notif.status.isGranted) { notif.launchPermissionRequest() }
        }
    }
}

@Composable
private fun PermRow(label: String, granted: Boolean, request: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold)
                Text(if (granted) "Granted" else "Not granted",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (!granted) {
                TextButton(onClick = request) { Text("Grant") }
            }
        }
    }
}