package com.example.echoai.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TranscriptionResponse(
    val sessionId: String,
    val chunkIndex: Int,
    val language: String,
    val lines: List<Line>
) {
    @JsonClass(generateAdapter = true)
    data class Line(
        val offsetMs: Int,
        val text: String
    )
}
