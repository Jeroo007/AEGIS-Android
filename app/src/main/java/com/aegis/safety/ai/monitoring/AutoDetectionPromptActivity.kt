package com.aegis.safety.presentation.monitoring

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aegis.safety.R
import com.aegis.safety.presentation.theme.AegisDanger
import com.aegis.safety.presentation.theme.AegisTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay

/**
 * Full-screen confirmation prompt. Shown over the lock screen.
 *
 *  • Configurable countdown (default 15 s, read from intent extra
 *    `confirmation_seconds`)
 *  • User taps "I'm OK" → dismissed, logged as false positive
 *  • User taps "Send help now" → SOS countdown starts immediately
 *  • Countdown reaches zero → SOS countdown starts automatically
 *
 * This is the human-in-the-loop guarantee. AI never silently fires SOS.
 *
 * Result codes:
 *   RESULT_OK           → user said "I'm OK" — false positive
 *   RESULT_FIRST_USER   → escalate (either explicit or timeout)
 */
@AndroidEntryPoint
class AutoDetectionPromptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager)
                .requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val level = intent.getStringExtra(EXTRA_LEVEL) ?: "HIGH_PRIORITY"
        val reason = intent.getStringExtra(EXTRA_REASON) ?: ""
        val fused = intent.getFloatExtra(EXTRA_FUSED, 0f)
        val totalSeconds = intent.getIntExtra(EXTRA_CONFIRMATION_SECONDS, 15)
            .coerceIn(MIN_SECONDS, MAX_SECONDS)

        setContent {
            AegisTheme {
                Surface(Modifier.fillMaxSize(), color = AegisDanger) {
                    PromptContent(
                        level = level,
                        reason = reason,
                        fused = fused,
                        totalSeconds = totalSeconds,
                        onImOk = {
                            setResult(RESULT_OK)
                            finish()
                        },
                        onSendHelp = {
                            setResult(RESULT_FIRST_USER)
                            finish()
                        },
                        onTimeout = {
                            setResult(RESULT_FIRST_USER)
                            finish()
                        }
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_LEVEL = "level"
        const val EXTRA_REASON = "reason"
        const val EXTRA_FUSED = "fused"
        const val EXTRA_CONFIRMATION_SECONDS = "confirmation_seconds"

        private const val MIN_SECONDS = 5
        private const val MAX_SECONDS = 60
    }
}

@Composable
private fun PromptContent(
    level: String,
    reason: String,
    fused: Float,
    totalSeconds: Int,
    onImOk: () -> Unit,
    onSendHelp: () -> Unit,
    onTimeout: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var remaining by rememberSaveable { mutableIntStateOf(totalSeconds) }
    val progress by animateFloatAsState(
        targetValue = remaining / totalSeconds.toFloat(),
        animationSpec = tween(400),
        label = "promptCountdown"
    )

    // Guard: fire onTimeout exactly once.
    var fired by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        while (remaining > 0) {
            delay(1000)
            remaining--
            if (remaining in 1..3) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        if (!fired) {
            fired = true
            onTimeout()
        }
    }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            stringResource(R.string.monitoring_prompt_title),
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.monitoring_prompt_subtitle),
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Confidence: ${(fused * 100).toInt()}% · Level: ${level.replace('_', ' ')}",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        if (reason.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                reason,
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(32.dp))

        Box(
            Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { progress },
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f),
                strokeWidth = 8.dp,
                modifier = Modifier.fillMaxSize()
            )
            Text(
                "$remaining",
                color = Color.White,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(40.dp))

        Button(
            onClick = onImOk,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = AegisDanger
            ),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.monitoring_prompt_im_ok),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onSendHelp,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(2.dp, Color.White),
            modifier = Modifier.fillMaxWidth().height(64.dp)
        ) {
            Text(
                stringResource(R.string.monitoring_prompt_send_help),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}