package com.echoai.app.data.remote

import java.io.File

interface TranscriptionDataSource {
    suspend fun transcribe(file: File, sessionId: Long, chunkIndex: Int): TranscriptionResponse
}
