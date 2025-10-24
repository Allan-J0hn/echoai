package com.example.echoai.data.local

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SessionState(
    val sessionId: Long,
    val startedAt: Long,
    val lastChunkIndex: Int,
    val status: SessionStatus,
    val startRealtimeMs: Long = 0L,
    val accumulatedElapsedMs: Long = 0L,
    val lastResumedRealtimeMs: Long? = null
)
