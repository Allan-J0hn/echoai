package com.example.echoai.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TranscriptionCoordinator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun enqueueOnChunkClosed(sessionId: Long, chunkIndex: Int, filePath: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<TranscribeChunkWorker>()
            .setConstraints(constraints)
            .setInputData(TranscribeChunkWorker.createWorkData(sessionId, chunkIndex, filePath))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "transcribe_${sessionId}_$chunkIndex",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    fun enqueueGenerateSummary(sessionId: Long) {
        val workRequest = OneTimeWorkRequestBuilder<GenerateSummaryWorker>()
            .setInputData(GenerateSummaryWorker.createWorkData(sessionId))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "generate_summary_$sessionId",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
    }
}
