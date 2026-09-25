package com.aegis.safety.core.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioCapture @Inject constructor() {

    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun frames(sampleRateHz: Int = 16_000, frameMs: Int = 40): Flow<ShortArray> = flow {
        val samplesPerFrame = sampleRateHz * frameMs / 1000
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRateHz,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBufferSize <= 0) {
            Timber.e("Invalid AudioRecord minBufferSize: $minBufferSize")
            return@flow
        }

        val bufferSize = maxOf(minBufferSize, samplesPerFrame * 2 * 2)
        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRateHz,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (e: SecurityException) {
            Timber.e(e, "Permission missing for AudioRecord")
            return@flow
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid AudioRecord configuration")
            return@flow
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Timber.e("AudioRecord failed to initialize")
            audioRecord.release()
            return@flow
        }

        try {
            audioRecord.startRecording()
            while (currentCoroutineContext().isActive) {
                val buffer = ShortArray(samplesPerFrame)
                var read = 0
                while (read < samplesPerFrame && currentCoroutineContext().isActive) {
                    val result = audioRecord.read(buffer, read, samplesPerFrame - read)
                    if (result > 0) {
                        read += result
                    } else if (result < 0) {
                        Timber.w("AudioRecord read error: $result")
                        break
                    }
                }
                if (read == samplesPerFrame) {
                    emit(buffer)
                }
            }
        } finally {
            try {
                if (audioRecord.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop()
                }
                audioRecord.release()
            } catch (e: Exception) {
                Timber.e(e, "Error releasing AudioRecord")
            }
        }
    }.flowOn(Dispatchers.IO)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startStreaming(): Flow<ShortArray> = frames()
}

