package com.aegis.safety.presentation.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aegis.safety.BuildConfig
import com.aegis.safety.R
import com.aegis.safety.presentation.components.AegisTopBar

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(topBar = { AegisTopBar(title = "About", onBack = onBack) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Image(
                painter = painterResource(R.drawable.ic_aegis_logo),
                contentDescription = "AEGIS Logo",
                modifier = Modifier.height(72.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Intelligent Safety. Instant Response.",
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Text("Safety principles", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            listOf(
                "AEGIS assists humans — it does not replace emergency services.",
                "AI never claims certainty.",
                "No face recognition, no identity inference.",
                "Raw audio/video is not uploaded by default.",
                "Manual SOS always escalates — regardless of AI confidence.",
                "Demo events are never mixed with real incidents."
            ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}