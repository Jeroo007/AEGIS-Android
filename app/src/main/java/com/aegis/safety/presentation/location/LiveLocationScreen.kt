package com.aegis.safety.presentation.location

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.R
import com.aegis.safety.domain.models.SafeZone
import com.aegis.safety.presentation.components.AegisTopBar
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveLocationScreen(
    onBack: () -> Unit,
    vm: LiveLocationViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // ---- API key diagnostics ----
    val apiKey = stringResource(R.string.google_maps_api_key)
    val isKeyPlaceholder = apiKey.isBlank() ||
        apiKey == "YOUR_GOOGLE_MAPS_API_KEY_HERE" ||
        !apiKey.startsWith("AIza")

    var showZones by remember { mutableStateOf(true) }
    var mapLoaded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* GoogleMap reads permission itself */ }
    LaunchedEffect(Unit) {
        permissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    val startLatLng = LatLng(
        s.current?.latitude ?: 13.0827,
        s.current?.longitude ?: 80.2707
    )
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startLatLng, 16f)
    }

    LaunchedEffect(s.current?.latitude, s.current?.longitude) {
        val loc = s.current ?: return@LaunchedEffect
        cameraState.animate(
            update = CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f),
            durationMs = 700
        )
    }

    Scaffold(
        topBar = {
            AegisTopBar(
                title = "Live Location",
                onBack = onBack,
                actions = {
                    IconButton(onClick = vm::manualRefreshZones) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh zones")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {

            // ---------- GOOGLE MAP ----------
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapType = MapType.NORMAL
                ),
                uiSettings = MapUiSettings(
                    myLocationButtonEnabled = true,
                    zoomControlsEnabled = true,
                    mapToolbarEnabled = true,
                    compassEnabled = true
                ),
                onMapLoaded = { mapLoaded = true }
            ) {
                s.current?.let { loc ->
                    Marker(
                        state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                        title = "You",
                        snippet = "±${loc.accuracyMeters.toInt()} m",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE)
                    )
                }

                if (showZones) {
                    s.safeZones.forEach { zone ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(zone.latitude, zone.longitude)
                            ),
                            title = zone.name,
                            snippet = "${zone.type.name.replace("_", " ")} · " +
                                "${zone.distanceMeters?.toInt() ?: 0} m",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                when (zone.type.name) {
                                    "POLICE_STATION" -> BitmapDescriptorFactory.HUE_BLUE
                                    "HOSPITAL" -> BitmapDescriptorFactory.HUE_RED
                                    "SECURITY_OFFICE" -> BitmapDescriptorFactory.HUE_ORANGE
                                    else -> BitmapDescriptorFactory.HUE_GREEN
                                }
                            )
                        )
                    }
                }
            }

            // ---------- API KEY ERROR BANNER ----------
            if (isKeyPlaceholder) {
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 6.dp
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text("Google Maps key not configured",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Add your key to res/values/strings.xml → " +
                                "google_maps_api_key, then rebuild. " +
                                "The map will stay blank until then.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                val uri = Uri.parse(
                                    "https://console.cloud.google.com/google/maps-apis")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }) { Text("Get key") }
                            TextButton(onClick = {
                                val uri = Uri.parse(
                                    "geo:${s.current?.latitude ?: 13.0827}," +
                                        "${s.current?.longitude ?: 80.2707}")
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }) { Text("Open in Maps") }
                        }
                    }
                }
            }

            // ---------- INFO CARD ----------
            Column(
                Modifier.fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = if (isKeyPlaceholder) 190.dp else 12.dp)
                    .padding(horizontal = 12.dp)
            ) {
                LocationInfoCard(s)
            }

            // ---------- SAFE ZONES BOTTOM SHEET ----------
            Surface(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shadowElevation = 8.dp
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Nearby Safe Places (${s.safeZones.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { showZones = !showZones }) {
                            Icon(
                                if (showZones) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle zones on map"
                            )
                        }
                    }

                    if (s.loadingZones) {
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
                    }

                    Spacer(Modifier.height(8.dp))

                    if (s.safeZones.isEmpty()) {
                        Text("Loading nearby safe places…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        s.safeZones.take(4).forEach { zone ->
                            CompactZoneRow(zone)
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationInfoCard(s: LiveLocationUiState) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 4.dp
    ) {
        Column(Modifier.padding(12.dp)) {
            val loc = s.current
            if (loc == null) {
                Text("Acquiring GPS…", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    "%.5f, %.5f".format(loc.latitude, loc.longitude),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "±${loc.accuracyMeters.toInt()} m · ${loc.provider ?: "gps"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Updated ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(loc.timestamp))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CompactZoneRow(zone: SafeZone) {
    val context = LocalContext.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (zone.type.name) {
                "POLICE_STATION" -> Icons.Default.LocalPolice
                "HOSPITAL" -> Icons.Default.LocalHospital
                "SECURITY_OFFICE" -> Icons.Default.Security
                else -> Icons.Default.Place
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(zone.name, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold)
            Text(
                "${zone.distanceMeters?.toInt() ?: 0} m · ETA ${zone.etaMinutes ?: "—"} min",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { navigateTo(context, zone) }) {
            Icon(Icons.Default.Directions, contentDescription = "Navigate")
        }
        zone.phoneNumber?.let { phone ->
            IconButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
            }) {
                Icon(Icons.Default.Phone, contentDescription = "Call")
            }
        }
    }
}

private fun navigateTo(context: android.content.Context, zone: SafeZone) {
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