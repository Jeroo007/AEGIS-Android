package com.aegis.safety.presentation.safezones

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.domain.models.SafeZone
import com.aegis.safety.domain.models.SafeZoneType
import com.aegis.safety.presentation.components.AegisTopBar

@Composable
fun SafeZonesScreen(
    onBack: () -> Unit,
    vm: SafeZonesViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            AegisTopBar(
                title = "Nearby Help",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                s.loading -> LoadingState()
                s.error != null -> ErrorState(s.error!!) { vm.refresh() }
                s.noResults -> NoResultsState(
                    radiusMeters = s.radiusMeters,
                    onExpand = { vm.expandRadius() },
                    onRefresh = { vm.refresh() }
                )
                else -> ResultsList(
                    s = s,
                    onNavigate = { zone ->
                        val uri = Uri.parse(
                            "geo:${zone.latitude},${zone.longitude}" +
                                "?q=${Uri.encode(zone.name)}"
                        )
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }
                    },
                    onCall = { zone ->
                        zone.phoneNumber?.let { phone ->
                            val uri = Uri.parse("tel:${phone.replace(" ", "")}")
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Locating nearby hospitals & police stations…")
    }
}

@Composable
private fun NoResultsState(
    radiusMeters: Float,
    onExpand: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LocationSearching,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Nothing found within ${(radiusMeters / 1000f).toInt()} km",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "You may be outside Coimbatore district coverage. " +
                "Try widening the search, or refresh if you've moved.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onExpand,
            enabled = radiusMeters < SafeZonesUiState.MAX_RADIUS_M,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.ZoomOutMap, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Search ${(radiusMeters * 2 / 1000f).toInt()} km")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Refresh")
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LocationOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ResultsList(
    s: SafeZonesUiState,
    onNavigate: (SafeZone) -> Unit,
    onCall: (SafeZone) -> Unit
) {
    val police = s.zones.filter { it.type == SafeZoneType.POLICE_STATION }
    val hospitals = s.zones.filter { it.type == SafeZoneType.HOSPITAL }
    val others = s.zones.filter {
        it.type != SafeZoneType.POLICE_STATION && it.type != SafeZoneType.HOSPITAL
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                "${s.zones.size} place(s) within " +
                    "${(s.radiusMeters / 1000f).toInt()} km · sorted by distance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (police.isNotEmpty()) {
            item {
                SectionHeader(
                    icon = Icons.Default.LocalPolice,
                    title = "Police Stations (${police.size})"
                )
            }
            items(police, key = { "p_${it.id}" }) { zone ->
                SafeZoneCard(zone, onNavigate, onCall)
            }
        }

        if (hospitals.isNotEmpty()) {
            item {
                SectionHeader(
                    icon = Icons.Default.LocalHospital,
                    title = "Hospitals (${hospitals.size})"
                )
            }
            items(hospitals, key = { "h_${it.id}" }) { zone ->
                SafeZoneCard(zone, onNavigate, onCall)
            }
        }

        if (others.isNotEmpty()) {
            item {
                SectionHeader(
                    icon = Icons.Default.Place,
                    title = "Other Safe Places (${others.size})"
                )
            }
            items(others, key = { "o_${it.id}" }) { zone ->
                SafeZoneCard(zone, onNavigate, onCall)
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        Modifier.padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SafeZoneCard(
    zone: SafeZone,
    onNavigate: (SafeZone) -> Unit,
    onCall: (SafeZone) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(iconFor(zone.type), contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(zone.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        zone.address ?: zone.type.name.replace('_', ' '),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (zone.verified) Icon(
                    Icons.Default.Verified,
                    contentDescription = "Verified",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                InfoBlock("Distance", formatDistance(zone.distanceMeters))
                InfoBlock("ETA", zone.etaMinutes?.let { "$it min" } ?: "—")
                InfoBlock("Status", if (zone.open24h) "Open 24h" else "Call ahead")
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onNavigate(zone) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Directions, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Navigate")
                }
                if (!zone.phoneNumber.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = { onCall(zone) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Call")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatDistance(meters: Float?): String = when {
    meters == null -> "—"
    meters < 1000f -> "${meters.toInt()} m"
    else -> "%.1f km".format(meters / 1000f)
}

private fun iconFor(type: SafeZoneType) = when (type) {
    SafeZoneType.POLICE_STATION -> Icons.Default.LocalPolice
    SafeZoneType.HOSPITAL -> Icons.Default.LocalHospital
    SafeZoneType.SECURITY_OFFICE, SafeZoneType.CAMPUS_SECURITY -> Icons.Default.Security
    SafeZoneType.PATROL_BASE -> Icons.Default.Shield
    SafeZoneType.TRANSIT -> Icons.Default.DirectionsTransit
    SafeZoneType.PARKING -> Icons.Default.LocalParking
    else -> Icons.Default.Place
}