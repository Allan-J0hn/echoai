package com.example.echoai.data.remote

import com.example.echoai.BuildConfig
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class RemoteTranscriptionDataSource @Inject constructor(
    private val transcriptionApi: TranscriptionApi
) : TranscriptionDataSource {

    override suspend fun transcribe(file: File, sessionId: Long, chunkIndex: Int): TranscriptionResponse {
        val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val sessionIdBody = sessionId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val chunkIndexBody = chunkIndex.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        return transcriptionApi.transcribe(
            file = body,
            sessionId = sessionIdBody,
            chunkIndex = chunkIndexBody,
            apiKey = "Bearer ${BuildConfig.TRANSCRIBE_KEY}"
        )
    }
}
