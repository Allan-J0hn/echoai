package com.echoai.app.di

import com.echoai.app.data.summary.MockSummaryDataSource
import com.echoai.app.data.summary.SummaryDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindSummaryDataSource(
        mockSummaryDataSource: MockSummaryDataSource
    ): SummaryDataSource
}
