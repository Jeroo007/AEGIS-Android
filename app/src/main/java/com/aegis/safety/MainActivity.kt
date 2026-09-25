package com.aegis.safety

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.aegis.safety.ai.monitoring.AegisMonitoringService
import com.aegis.safety.presentation.monitoring.AutoDetectionPromptActivity
import com.aegis.safety.presentation.navigation.AegisNavHost
import com.aegis.safety.presentation.theme.AegisTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // -------------------------------------------------------------------------
    // Auto-detection prompt result handler
    // -------------------------------------------------------------------------

    /**
     * Launches [AutoDetectionPromptActivity] and reacts to the user's choice.
     *
     * RESULT_OK           → user pressed "I'm OK" — false positive, no escalation.
     * RESULT_FIRST_USER   → user pressed "SEND HELP NOW" OR countdown expired.
     *                       Route to the SOS countdown via a deeplink extra.
     * Anything else       → treat as dismissed; no escalation.
     */
    private val autoDetectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            RESULT_OK -> {
                Timber.i("Auto-detection prompt: user said I'm OK — logged as false positive.")
            }
            RESULT_FIRST_USER -> {
                Timber.i("Auto-detection prompt: escalating to SOS countdown.")
                // Deeplink through MainActivity so NavHost can route to SOS.
                // The NavHost already handles "sos:voice_trigger"; we reuse the
                // same mechanism with a different source tag.
                val deepLink = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("deeplink", "sos:auto_detection")
                }
                startActivity(deepLink)
            }
            else -> {
                Timber.i("Auto-detection prompt dismissed without action.")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Broadcast receiver for auto-detection escalations
    // -------------------------------------------------------------------------

    private val autoEscalationReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != AegisMonitoringService.ACTION_AUTO_ESCALATION) return
            val level = intent.getStringExtra("level") ?: "HIGH_PRIORITY"
            val fused = intent.getFloatExtra("fused", 0f)
            val reason = intent.getStringExtra("reason") ?: ""
            Timber.i("Auto-escalation received: level=%s fused=%.2f", level, fused)

            val prompt = Intent(this@MainActivity, AutoDetectionPromptActivity::class.java).apply {
                putExtra(AutoDetectionPromptActivity.EXTRA_LEVEL, level)
                putExtra(AutoDetectionPromptActivity.EXTRA_FUSED, fused)
                putExtra(AutoDetectionPromptActivity.EXTRA_REASON, reason)
                // Override with the user's configured confirmation window.
                // We don't have it here; the activity falls back to 15.
                // A future improvement: pass it via the broadcast extra.
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            autoDetectionLauncher.launch(prompt)
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        registerAutoEscalationReceiver()

        setContent {
            AegisTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AegisNavHost()
                }
            }
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(autoEscalationReceiver) }
            .onFailure { Timber.w(it, "autoEscalationReceiver was not registered") }
        super.onDestroy()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun registerAutoEscalationReceiver() {
        val filter = IntentFilter(AegisMonitoringService.ACTION_AUTO_ESCALATION)
        ContextCompat.registerReceiver(
            this,
            autoEscalationReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}