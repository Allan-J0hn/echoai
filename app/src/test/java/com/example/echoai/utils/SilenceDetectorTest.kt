package com.example.echoai.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SilenceDetectorTest {

    @Test
    fun `test silence detection`() {
        val silenceDetector = SilenceDetector(16000, 200, 1000)
        // Calibrate
        repeat(10) {
            assertFalse(silenceDetector.onData(ShortArray(3200) { 1000 }))
        }

        // Silence
        var isSilent = false
        repeat(5) {
            isSilent = silenceDetector.onData(ShortArray(3200) { 100 })
        }
        assertTrue(isSilent)

        // Not silent
        assertFalse(silenceDetector.onData(ShortArray(3200) { 1000 }))
    }
}
