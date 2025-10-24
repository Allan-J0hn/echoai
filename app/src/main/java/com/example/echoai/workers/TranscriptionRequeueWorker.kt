package com.example.echoai.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.echoai.domain.RecordingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class TranscriptionRequeueWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val recordingRepository: RecordingRepository,
    private val transcriptionCoordinator: TranscriptionCoordinator
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val sessionsWithChunks = recordingRepository.getAllSessionsWithChunks()
            
            sessionsWithChunks.forEach { sessionWithChunks ->
                val untranscribedChunks = sessionWithChunks.chunks.filter { !it.transcribed }
                
                untranscribedChunks.forEach { chunk ->
                    transcriptionCoordinator.enqueueOnChunkClosed(
                        sessionId = chunk.sessionId,
                        chunkIndex = chunk.index,
                        filePath = chunk.filePath
                    )
                }
            }
            
            return@withContext Result.success()
        } catch (e: Exception) {
            // If any unexpected error occurs, retry the job
            return@withContext Result.retry()
        }
    }
}
