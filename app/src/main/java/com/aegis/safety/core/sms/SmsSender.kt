package com.aegis.safety.core.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.aegis.safety.domain.models.EmergencyContact
import com.aegis.safety.domain.models.Incident
import com.aegis.safety.domain.models.LocationSample
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends SOS SMS to emergency contacts.
 *
 * If SEND_SMS permission is granted, sends silently.
 * Otherwise returns 0 — the app should fall back to the SMS intent flow.
 */
@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Sends an SOS SMS to every contact that has notifyOnSos = true.
     * Returns the number of SMS actually handed to the telephony stack.
     */
    fun sendSosSms(
        contacts: List<EmergencyContact>,
        incident: Incident,
        location: LocationSample?,
        senderName: String
    ): Int {
        if (!hasSmsPermission()) {
            Timber.w("SEND_SMS permission not granted — skipping automatic SMS")
            return 0
        }
        val recipients = contacts.filter { it.notifyOnSos && it.phoneNumber.isNotBlank() }
        if (recipients.isEmpty()) {
            Timber.i("No emergency contacts to notify")
            return 0
        }

        val message = buildMessage(incident, location, senderName)
        val smsManager = smsManager()
        var sent = 0

        for (c in recipients) {
            try {
                val parts = smsManager.divideMessage(message)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(
                        c.phoneNumber, null, parts, null, null
                    )
                } else {
                    smsManager.sendTextMessage(c.phoneNumber, null, message, null, null)
                }
                sent++
                Timber.i("SOS SMS sent to ${c.name} (${c.phoneNumber})")
            } catch (t: Throwable) {
                Timber.e(t, "Failed to send SOS SMS to ${c.name}")
            }
        }
        return sent
    }

    /** Builds a short (< 300 char) SOS message. */
    fun buildMessage(
        incident: Incident,
        location: LocationSample?,
        senderName: String
    ): String {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val sb = StringBuilder()
        sb.append("AEGIS EMERGENCY\n")
        sb.append("$senderName triggered SOS.\n")
        if (location != null) {
            sb.append("Location: https://maps.google.com/?q=")
                .append(location.latitude)
                .append(",")
                .append(location.longitude)
                .append("\n")
            sb.append("Accuracy: ±${location.accuracyMeters.toInt()} m\n")
        } else {
            sb.append("Location: unavailable\n")
        }
        sb.append("Time: $time\n")
        sb.append("Incident: ${incident.id.take(8)}")
        return sb.toString()
    }

    private fun smsManager(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
}