package com.example.echoai.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.echoai.data.local.Summary
import com.example.echoai.data.local.SummaryStatus
import com.example.echoai.data.summary.SummaryDataSource
import com.example.echoai.domain.RecordingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class GenerateSummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val summaryDataSource: SummaryDataSource,
    private val recordingRepository: RecordingRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sessionId = inputData.getLong(KEY_SESSION_ID, -1)
        Log.d("SummaryWorker", "Start session=$sessionId")
        if (sessionId == -1L) {
            return@withContext Result.failure()
        }

        try {
            Log.d("SummaryWorker", "Upsert status=GENERATING")
            recordingRepository.upsertSummary(
                Summary(
                    sessionId = sessionId,
                    title = "",
                    summaryText = "",
                    actionItemsJson = "[]",
                    keyPointsJson = "[]",
                    status = SummaryStatus.GENERATING,
                    error = null
                )
            )

            val transcriptLines = recordingRepository.getTranscriptLinesForSession(sessionId)
            
            if (transcriptLines.isEmpty()) {
                Log.d("SummaryWorker", "No transcript lines, creating default summary.")
                recordingRepository.upsertSummary(
                    Summary(
                        sessionId = sessionId,
                        title = "Session Summary",
                        summaryText = "No transcript is available for this session yet.",
                        status = SummaryStatus.DONE
                    )
                )
            } else {
                val summary = summaryDataSource.generateSummary(sessionId, transcriptLines)
                recordingRepository.upsertSummary(summary)
            }
            
            Log.d("SummaryWorker", "Upsert status=COMPLETED")
            Result.success()
        } catch (e: Exception) {
            Log.e("SummaryWorker", "Summary failed", e)
            recordingRepository.upsertSummary(
                Summary(
                    sessionId = sessionId,
                    title = "",
                    summaryText = "",
                    actionItemsJson = "[]",
                    keyPointsJson = "[]",
                    status = SummaryStatus.ERROR,
                    error = e.message ?: "Unknown error"
                )
            )
            Result.failure()
        }
    }

    companion object {
        const val KEY_SESSION_ID = "sessionId"

        fun createWorkData(sessionId: Long) = workDataOf(
            KEY_SESSION_ID to sessionId
        )
    }
}
