package com.echoai.app.domain

import com.echoai.app.service.PauseReason

sealed class RecordingStatus {
    data object Recording : RecordingStatus()
    data class Paused(val reason: PauseReason) : RecordingStatus()
    data class Warning(val message: String) : RecordingStatus()
    data object Stopped : RecordingStatus()
    data class Error(val message: String) : RecordingStatus()
}
