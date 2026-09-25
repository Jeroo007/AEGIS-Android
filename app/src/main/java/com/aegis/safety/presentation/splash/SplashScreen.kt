package com.aegis.safety.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aegis.safety.R
import com.aegis.safety.presentation.theme.AegisAccent
import com.aegis.safety.presentation.theme.AegisPrimary
import com.aegis.safety.presentation.theme.AegisPrimaryDark

@Composable
fun SplashScreen(
    onNavigate: (SplashDestination) -> Unit,
    vm: SplashViewModel = hiltViewModel()
) {
    val destination by vm.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) { destination?.let(onNavigate) }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(AegisPrimary, AegisPrimaryDark))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_aegis_logo_white),
                contentDescription = "AEGIS Logo",
                modifier = Modifier.fillMaxWidth(0.6f).heightIn(max = 160.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Intelligent Safety. Instant Response.",
                style = MaterialTheme.typography.titleMedium,
                color = AegisAccent, textAlign = TextAlign.Center)
        }
    }
}