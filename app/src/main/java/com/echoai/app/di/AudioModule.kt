package com.echoai.app.di

import android.content.Context
import com.echoai.app.data.local.AppDatabase
import com.echoai.app.data.local.AudioRecordEngine
import com.echoai.app.data.local.RecordingRepositoryImpl
import com.echoai.app.domain.AudioEngine
import com.echoai.app.domain.RecordingRepository
import com.echoai.app.utils.SilenceDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
        appDatabase: AppDatabase,
        @ApplicationContext context: Context
    ): RecordingRepository = RecordingRepositoryImpl(appDatabase, context)

    @Provides
    @Singleton
    fun provideSilenceDetector(): SilenceDetector {
        return SilenceDetector(sampleRate = DEFAULT_SAMPLE_RATE)
    }
}
