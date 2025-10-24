package com.example.echoai.data.remote

import kotlinx.coroutines.delay
import java.io.File
import javax.inject.Inject

class MockTranscriptionDataSource @Inject constructor() : TranscriptionDataSource {

    override suspend fun transcribe(file: File, sessionId: Long, chunkIndex: Int): TranscriptionResponse {
        delay(1000)
        return TranscriptionResponse(
            sessionId = sessionId.toString(),
            chunkIndex = chunkIndex,
            language = "en",
            lines = listOf(
                TranscriptionResponse.Line(0, "Hello, world!"),
                TranscriptionResponse.Line(1200, "This is a mock transcription.")
            )
        )
    }
}
