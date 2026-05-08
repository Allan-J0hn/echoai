package com.example.echoai.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.echoai.data.local.SessionStatus
import com.example.echoai.data.local.TranscriptLine
import com.example.echoai.data.remote.TranscriptionDataSource
import com.example.echoai.domain.RecordingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

@HiltWorker
class TranscribeChunkWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transcriptionDataSource: TranscriptionDataSource,
    private val recordingRepository: RecordingRepository,
    private val transcriptionCoordinator: TranscriptionCoordinator
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sessionId = inputData.getLong("sessionId", -1)
        val chunkIndex = inputData.getInt("chunkIndex", -1)
        val filePath = inputData.getString("filePath")

        if (sessionId == -1L || chunkIndex == -1 || filePath == null) {
            return@withContext Result.failure()
        }

        val chunk = recordingRepository.getSessionWithChunks(sessionId)?.chunks?.find { it.index == chunkIndex }
            ?: return@withContext Result.failure()
        if (chunk.transcribed) return@withContext Result.success()

        val file = File(filePath)
        if (!file.exists()) return@withContext Result.retry()

        try {
            val resp = transcriptionDataSource.transcribe(file, sessionId, chunkIndex)
            recordingRepository.insertTranscriptLines(
                resp.lines.map { line ->
                    TranscriptLine(
                        id = "${sessionId}_${chunkIndex}_${line.offsetMs}_${abs(line.text.hashCode())}",
                        sessionId = sessionId,
                        chunkIndex = chunkIndex,
                        offsetMs = line.offsetMs,
                        text = line.text
                    )
                }
            )
            recordingRepository.markChunkUploaded(sessionId, chunkIndex, true)
            recordingRepository.markChunkTranscribed(sessionId, chunkIndex, true)
            enqueueSummaryIfReady(sessionId)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun enqueueSummaryIfReady(sessionId: Long) {
        val session = recordingRepository.getSessionWithChunks(sessionId) ?: return
        if (session.session.status == SessionStatus.STOPPED &&
            session.chunks.isNotEmpty() &&
            session.chunks.all { it.transcribed }
        ) {
            transcriptionCoordinator.enqueueGenerateSummary(sessionId)
        }
    }

    companion object {
        fun createWorkData(sessionId: Long, chunkIndex: Int, filePath: String) = workDataOf(
            "sessionId" to sessionId,
            "chunkIndex" to chunkIndex,
            "filePath" to filePath
        )
    }
}
