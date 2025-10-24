package com.example.echoai.data.remote

import java.io.File

interface TranscriptionDataSource {
    suspend fun transcribe(file: File, sessionId: Long, chunkIndex: Int): TranscriptionResponse
}
