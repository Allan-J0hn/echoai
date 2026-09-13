package com.echoai.app.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class SilenceDetectorTest {

    @Test
    fun `test silence detection`() {
        val silenceDetector = SilenceDetector(16000, 200)
        val roomTone = pcm16(ShortArray(3200) { 100 })
        val silence = pcm16(ShortArray(3200) { 0 })
        val speech = pcm16(ShortArray(3200) { 1000 })

        // Calibrate
        repeat(10) {
            assertEquals(SilenceEvent.NoChange, silenceDetector.onData(roomTone))
        }

        // Silence
        repeat(49) {
            assertEquals(SilenceEvent.NoChange, silenceDetector.onData(silence))
        }
        assertEquals(SilenceEvent.SilentFor10s, silenceDetector.onData(silence))

        // Not silent
        assertEquals(SilenceEvent.SoundResumed, silenceDetector.onData(speech))
    }

    private fun pcm16(samples: ShortArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        samples.forEachIndexed { index, sample ->
            bytes[index * 2] = sample.toByte()
            bytes[index * 2 + 1] = (sample.toInt() shr 8).toByte()
        }
        return bytes
    }
}
