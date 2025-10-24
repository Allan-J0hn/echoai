package com.example.echoai.di

import com.example.echoai.BuildConfig
import com.example.echoai.data.remote.MockTranscriptionDataSource
import com.example.echoai.data.remote.RemoteTranscriptionDataSource
import com.example.echoai.data.remote.TranscriptionApi
import com.example.echoai.data.remote.TranscriptionDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
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
        return Retrofit.Builder()
            .baseUrl(BuildConfig.TRANSCRIBE_URL)
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
        transcriptionApi: TranscriptionApi,
        mockTranscriptionDataSource: MockTranscriptionDataSource
    ): TranscriptionDataSource {
        return if (BuildConfig.TRANSCRIBE_URL.isNotEmpty() && BuildConfig.TRANSCRIBE_KEY.isNotEmpty()) {
            RemoteTranscriptionDataSource(transcriptionApi)
        } else {
            mockTranscriptionDataSource
        }
    }
}
