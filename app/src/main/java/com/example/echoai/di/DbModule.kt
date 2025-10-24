package com.example.echoai.di

import android.content.Context
import androidx.room.Room
import com.example.echoai.data.local.AppDatabase
import com.example.echoai.data.local.AudioChunkDao
import com.example.echoai.data.local.SessionDao
import com.example.echoai.data.local.SummaryDao
import com.example.echoai.data.local.TranscriptLineDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "echoai.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    }

    @Provides
    @Singleton
    fun provideSessionDao(appDatabase: AppDatabase): SessionDao {
        return appDatabase.sessionDao()
    }

    @Provides
    @Singleton
    fun provideAudioChunkDao(appDatabase: AppDatabase): AudioChunkDao {
        return appDatabase.audioChunkDao()
    }

    @Provides
    @Singleton
    fun provideTranscriptLineDao(appDatabase: AppDatabase): TranscriptLineDao {
        return appDatabase.transcriptLineDao()
    }

    @Provides
    @Singleton
    fun provideSummaryDao(appDatabase: AppDatabase): SummaryDao {
        return appDatabase.summaryDao()
    }
}
