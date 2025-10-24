package com.example.echoai.data.local

import androidx.room.withTransaction
import com.example.echoai.domain.RecordingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.math.max

class RecordingRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase
) : RecordingRepository {

    private val sessionDao = appDatabase.sessionDao()
    private val audioChunkDao = appDatabase.audioChunkDao()
    private val transcriptLineDao = appDatabase.transcriptLineDao()
    private val summaryDao = appDatabase.summaryDao()

    override suspend fun createNewSession(): Long {
        val session = Session(
            startTime = System.currentTimeMillis(),
            endTime = null,
            title = "Recording ${System.currentTimeMillis()}",
            status = SessionStatus.RECORDING
        )
        return sessionDao.insert(session)
    }

    override suspend fun addAudioChunk(audioChunk: AudioChunk) {
        audioChunkDao.insert(audioChunk)
    }

    override suspend fun finishSession(sessionId: Long) {
        appDatabase.withTransaction {
            val session = sessionDao.getSessionWithChunks(sessionId)?.session
            if (session != null && session.status != SessionStatus.STOPPED) {
                val endTime = System.currentTimeMillis()
                val durationSec = max(0, ((endTime - session.startTime) / 1000)).toInt()
                sessionDao.finalizeSession(sessionId, SessionStatus.STOPPED, endTime, durationSec)
            }
        }
    }

    override fun observeSessions(): Flow<List<SessionWithChunks>> {
        return sessionDao.getAllSessionsWithChunks()
    }

    override suspend fun getAllSessionsWithChunks(): List<SessionWithChunks> {
        return sessionDao.getAllSessionsWithChunksSuspend()
    }

    override suspend fun updateSessionStatus(id: Long, status: SessionStatus) {
        sessionDao.updateSessionStatus(id, status)
    }

    override suspend fun updateChunkFlags(
        sessionId: Long,
        index: Int,
        uploaded: Boolean,
        transcribed: Boolean
    ) {
        appDatabase.withTransaction {
            val chunk = audioChunkDao.getChunk(sessionId, index)
            chunk?.let {
                audioChunkDao.updateChunk(it.copy(uploaded = uploaded, transcribed = transcribed))
            }
        }
    }

    override suspend fun getSessionWithChunks(id: Long): SessionWithChunks? {
        return sessionDao.getSessionWithChunks(id)
    }

    override fun observeTranscript(sessionId: Long): Flow<List<TranscriptLine>> {
        return transcriptLineDao.observeTranscript(sessionId)
    }

    override suspend fun getTranscriptLinesForSession(sessionId: Long): List<String> {
        return transcriptLineDao.getTranscriptLinesForSession(sessionId)
    }

    override suspend fun insertTranscriptLines(lines: List<TranscriptLine>) {
        transcriptLineDao.upsertAll(lines)
    }

    override suspend fun markChunkUploaded(sessionId: Long, index: Int, uploaded: Boolean) {
        audioChunkDao.markChunkUploaded(sessionId, index, uploaded)
    }

    override suspend fun markChunkTranscribed(sessionId: Long, index: Int, transcribed: Boolean) {
        audioChunkDao.markChunkTranscribed(sessionId, index, transcribed)
    }

    override suspend fun findNextUntranscribedChunks(sessionId: Long): List<AudioChunk> {
        return audioChunkDao.findNextUntranscribedChunks(sessionId)
    }

    override fun observeSummary(sessionId: Long): Flow<Summary?> {
        return summaryDao.observeSummary(sessionId)
    }

    override suspend fun upsertSummary(summary: Summary) {
        summaryDao.upsert(summary)
    }

    override suspend fun deleteSession(id: Long) {
        sessionDao.deleteSessionById(id)
    }
}
