package com.example.echoai.di

import com.example.echoai.data.local.AppDatabase
import com.example.echoai.data.local.AudioRecordEngine
import com.example.echoai.data.local.RecordingRepositoryImpl
import com.example.echoai.domain.AudioEngine
import com.example.echoai.domain.RecordingRepository
import com.example.echoai.utils.SilenceDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    private const val DEFAULT_SAMPLE_RATE = 16000 // Common sample rate for audio recording

    @Provides
    @Singleton
    fun provideAudioEngine(): AudioEngine = AudioRecordEngine()

    @Provides
    @Singleton
    fun provideRecordingRepo(
        appDatabase: AppDatabase
    ): RecordingRepository = RecordingRepositoryImpl(appDatabase)

    @Provides
    @Singleton
    fun provideSilenceDetector(): SilenceDetector {
        return SilenceDetector(sampleRate = DEFAULT_SAMPLE_RATE)
    }
}
