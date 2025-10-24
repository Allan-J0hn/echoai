package com.example.echoai.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface TranscriptionApi {
    @Multipart
    @POST("v1/transcribe")
    suspend fun transcribe(
        @Part file: MultipartBody.Part,
        @Part("sessionId") sessionId: RequestBody,
        @Part("chunkIndex") chunkIndex: RequestBody,
        @Header("Authorization") apiKey: String
    ): TranscriptionResponse
}
