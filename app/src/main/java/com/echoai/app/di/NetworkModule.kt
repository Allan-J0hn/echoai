package com.echoai.app.di

import com.echoai.app.BuildConfig
import com.echoai.app.data.remote.MockTranscriptionDataSource
import com.echoai.app.data.remote.RemoteTranscriptionDataSource
import com.echoai.app.data.remote.TranscriptionApi
import com.echoai.app.data.remote.TranscriptionDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val baseUrl = BuildConfig.TRANSCRIBE_URL.trim()
        require(baseUrl.isNotEmpty()) { "TRANSCRIBE_URL is required for remote transcription." }
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTranscriptionApi(retrofit: Retrofit): TranscriptionApi {
        return retrofit.create(TranscriptionApi::class.java)
    }

    @Provides
    @Singleton
    fun provideTranscriptionDataSource(
        transcriptionApiProvider: Provider<TranscriptionApi>,
        mockTranscriptionDataSource: MockTranscriptionDataSource
    ): TranscriptionDataSource {
        return if (BuildConfig.TRANSCRIBE_URL.isNotBlank() && BuildConfig.TRANSCRIBE_KEY.isNotBlank()) {
            RemoteTranscriptionDataSource(transcriptionApiProvider.get())
        } else {
            mockTranscriptionDataSource
        }
    }
}
