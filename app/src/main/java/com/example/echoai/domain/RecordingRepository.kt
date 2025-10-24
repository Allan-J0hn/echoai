package com.example.echoai.domain

import com.example.echoai.data.local.*
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    suspend fun createNewSession(): Long
    suspend fun addAudioChunk(audioChunk: AudioChunk)
    suspend fun finishSession(sessionId: Long)
    fun observeSessions(): Flow<List<SessionWithChunks>>
    suspend fun getAllSessionsWithChunks(): List<SessionWithChunks>
    suspend fun updateSessionStatus(id: Long, status: SessionStatus)
    suspend fun updateChunkFlags(sessionId: Long, index: Int, uploaded: Boolean, transcribed: Boolean)
    suspend fun getSessionWithChunks(id: Long): SessionWithChunks?
    fun observeTranscript(sessionId: Long): Flow<List<TranscriptLine>>
    suspend fun getTranscriptLinesForSession(sessionId: Long): List<String>
    suspend fun insertTranscriptLines(lines: List<TranscriptLine>)
    suspend fun markChunkUploaded(sessionId: Long, index: Int, uploaded: Boolean)
    suspend fun markChunkTranscribed(sessionId: Long, index: Int, transcribed: Boolean)
    suspend fun findNextUntranscribedChunks(sessionId: Long): List<AudioChunk>
    fun observeSummary(sessionId: Long): Flow<Summary?>
    suspend fun upsertSummary(summary: Summary)
    suspend fun deleteSession(id: Long)
}
