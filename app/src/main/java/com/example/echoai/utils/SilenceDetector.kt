package com.example.echoai.utils

import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.sqrt

// Sealed class to represent silence detection events
sealed class SilenceEvent {
    data object SilentFor10s : SilenceEvent()
    data object SoundResumed : SilenceEvent()
    data object NoChange : SilenceEvent()
}

class SilenceDetector(
    private val sampleRate: Int,
    private val windowMs: Int = 200 // Process RMS in 200ms windows
) {
    private val windowSizeSamples = (sampleRate * windowMs / 1000)
    private val minThreshold = 150.0 // Absolute floor for RMS threshold

    // Calibration state
    private val calibrationDurationMs = 2000 // Calibrate for first 2 seconds
    private val calibrationWindowsCount = calibrationDurationMs / windowMs
    private val rmsValuesDuringCalibration = mutableListOf<Double>()
    private var isCalibrated = false
    private var noiseFloor: Double = 0.0

    // Silence detection state
    private var silentMs: Long = 0L // Accumulated silent time in milliseconds
    private var wasSilentFor10s = false // True if the last state reported was silent for 10s
    private var audioBuffer = ShortArray(0) // Buffer for incoming audio samples

    // Returns a SilenceEvent indicating a change in silence status
    fun onData(data: ByteArray): SilenceEvent {
        val newSamples = toShortArray(data)
        audioBuffer = audioBuffer.plus(newSamples) // Efficiently append new samples

        var event: SilenceEvent = SilenceEvent.NoChange

        // Process full windows from the buffer
        while (audioBuffer.size >= windowSizeSamples) {
            val window = audioBuffer.copyOfRange(0, windowSizeSamples)
            val rms = calculateRms(window)

            if (!isCalibrated) {
                rmsValuesDuringCalibration.add(rms)
                if (rmsValuesDuringCalibration.size >= calibrationWindowsCount) {
                    calibrateNoiseFloor()
                    isCalibrated = true
                    Timber.d("SilenceDetector: Calibrated noise floor: %.2f", noiseFloor)
                }
                // Discard processed window
                audioBuffer = audioBuffer.copyOfRange(windowSizeSamples, audioBuffer.size)
                continue // Don't detect silence during calibration
            }

            val threshold = max(noiseFloor * 1.15, minThreshold)
            val isCurrentlySilentWindow = rms < threshold

            if (isCurrentlySilentWindow) {
                silentMs += windowMs
            } else {
                silentMs = 0L // Reset if sound is detected
            }

            val nowSilentFor10s = silentMs >= 10_000L

            if (nowSilentFor10s && !wasSilentFor10s) {
                event = SilenceEvent.SilentFor10s
                Timber.d("SilenceDetector: Became silent for 10s. RMS=%.2f, Threshold=%.2f", rms, threshold)
            } else if (!nowSilentFor10s && wasSilentFor10s) {
                event = SilenceEvent.SoundResumed
                Timber.d("SilenceDetector: Sound resumed. RMS=%.2f, Threshold=%.2f", rms, threshold)
            }
            wasSilentFor10s = nowSilentFor10s

            // Discard processed window
            audioBuffer = audioBuffer.copyOfRange(windowSizeSamples, audioBuffer.size)
        }
        return event
    }

    // Reset detector state for a new recording session or process death recovery
    fun reset() {
        audioBuffer = ShortArray(0)
        silentMs = 0L
        wasSilentFor10s = false
        isCalibrated = false
        rmsValuesDuringCalibration.clear()
        noiseFloor = 0.0
        Timber.d("SilenceDetector: Reset.")
    }

    private fun calculateRms(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        var sumOfSquares = 0.0
        for (sample in samples) {
            sumOfSquares += sample.toDouble() * sample.toDouble()
        }
        return sqrt(sumOfSquares / samples.size)
    }

    private fun toShortArray(byteArray: ByteArray): ShortArray {
        val shortArray = ShortArray(byteArray.size / 2)
        ByteBuffer.wrap(byteArray).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)
        return shortArray
    }

    private fun calibrateNoiseFloor() {
        if (rmsValuesDuringCalibration.isNotEmpty()) {
            // Use median to be robust against initial spikes
            val sortedRms = rmsValuesDuringCalibration.sorted()
            noiseFloor = if (sortedRms.size % 2 == 1) {
                sortedRms[sortedRms.size / 2]
            } else {
                (sortedRms[sortedRms.size / 2 - 1] + sortedRms[sortedRms.size / 2]) / 2.0
            }
            // Ensure a minimum noise floor even in extremely quiet environments
            noiseFloor = max(noiseFloor, 50.0) // A very low absolute floor for calibration
        } else {
            noiseFloor = 50.0 // Default if no data during calibration
        }
    }
}
