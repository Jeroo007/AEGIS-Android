package com.aegis.safety.presentation.sos

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.safety.domain.models.Incident
import com.aegis.safety.domain.models.SafeZone
import com.aegis.safety.presentation.theme.AegisDanger

/**
 * Stateless — renders the active emergency using the parent SosViewModel's state.
 * This avoids the Compose Navigation ViewModel-scope pitfall where each screen
 * gets its own ViewModel instance (which was losing the SMS result).
 */
@Composable
fun ActiveEmergencyContent(
    incident: Incident,
    state: SosUiState,
    onResolve: () -> Unit
) {
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(AegisDanger)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Icon(Icons.Default.Warning, contentDescription = null,
            tint = Color.White, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text("EMERGENCY ACTIVE", color = Color.White,
            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Incident ${incident.id.take(8)}",
            color = Color.White.copy(alpha = 0.85f),
            style = MaterialTheme.typography.bodyMedium)

        Spacer(Modifier.height(20.dp))

        StatusItem("Alert created")
        StatusItem("Dispatcher notified")

        // ---- SMS status ----
        when {
            !state.smsAttempted ->
                StatusItem("Preparing SMS to contacts…", ok = null)
            state.smsTotalContacts == 0 ->
                StatusItem("No emergency contacts to notify", ok = false)
            state.smsSentCount > 0 ->
                StatusItem("SMS sent to ${state.smsSentCount} of ${state.smsTotalContacts} contacts", ok = true)
            state.smsTotalContacts > 0 ->
                StatusItem("Sending SMS to ${state.smsTotalContacts} contacts…", ok = null)
        }

        StatusItem("Live location sharing active")

        // ---- Live location card ----
        if (incident.latitude != null && incident.longitude != null) {
            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(14.dp),
                color = Color.White.copy(alpha = 0.15f)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Live location", color = Color.White,
                        style = MaterialTheme.typography.labelLarge)
                    Text(
                        "%.5f, %.5f".format(incident.latitude, incident.longitude),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("±${incident.accuracyMeters?.toInt() ?: 0} m",
                        color = Color.White.copy(alpha = 0.85f))
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ---- Nearest help ----
        Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
            Column(Modifier.padding(14.dp).fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NEAREST HELP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AegisDanger)
                    if (state.loadingSafeZones) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AegisDanger
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Police stations & hospitals closest to you",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(10.dp))

                if (state.nearestSafeZones.isEmpty() && !state.loadingSafeZones) {
                    Text("No safe places available nearby",
                        style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                } else {
                    state.nearestSafeZones.forEach { zone ->
                        EmergencyZoneRow(zone, context)
                        HorizontalDivider(
                            Modifier.padding(vertical = 6.dp),
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onResolve,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = AegisDanger
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("I'M SAFE — RESOLVE", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun StatusItem(text: String, ok: Boolean? = true) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, tint) = when (ok) {
            true -> Icons.Default.CheckCircle to Color.White
            false -> Icons.Default.Info to Color.White
            null -> Icons.Default.Sync to Color.White
        }
        Icon(icon, contentDescription = null, tint = tint,
            modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EmergencyZoneRow(zone: SafeZone, context: android.content.Context) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            when (zone.type.name) {
                "POLICE_STATION" -> Icons.Default.LocalPolice
                "HOSPITAL" -> Icons.Default.LocalHospital
                "SECURITY_OFFICE" -> Icons.Default.Security
                else -> Icons.Default.Place
            },
            contentDescription = null,
            tint = AegisDanger,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(zone.name, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge, color = Color.Black)
            Text(
                "${zone.distanceMeters?.toInt() ?: 0} m · ETA ${zone.etaMinutes ?: "—"} min" +
                    if (zone.open24h) " · Open 24h" else "",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray
            )
            zone.address?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
        FilledTonalIconButton(onClick = { openNavigation(context, zone) }) {
            Icon(Icons.Default.Directions, contentDescription = "Navigate")
        }
        Spacer(Modifier.width(6.dp))
        FilledTonalIconButton(onClick = {
            val tel = zone.phoneNumber ?: if (zone.type.name == "HOSPITAL") "108" else "100"
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel")))
        }) {
            Icon(Icons.Default.Phone, contentDescription = "Call")
        }
    }
}

private fun openNavigation(context: android.content.Context, zone: SafeZone) {
    val gnav = Intent(Intent.ACTION_VIEW,
        Uri.parse("google.navigation:q=${zone.latitude},${zone.longitude}&mode=d"))
        .apply { setPackage("com.google.android.apps.maps") }
    if (gnav.resolveActivity(context.packageManager) != null) {
        context.startActivity(gnav)
    } else {
        context.startActivity(
            Intent(Intent.ACTION_VIEW,
                Uri.parse("geo:${zone.latitude},${zone.longitude}?q=${Uri.encode(zone.name)}"))
        )
    }
}