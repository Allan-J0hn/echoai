package com.example.echoai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: Session): Long

    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<Session>>

    @Transaction
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessionsWithChunks(): Flow<List<SessionWithChunks>>
    
    @Transaction
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    suspend fun getAllSessionsWithChunksSuspend(): List<SessionWithChunks>

    @Query("UPDATE sessions SET status = :status WHERE id = :id")
    suspend fun updateSessionStatus(id: Long, status: SessionStatus)

    @Transaction
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionWithChunks(id: Long): SessionWithChunks?

    @Query("UPDATE sessions SET status=:status, endTime=:endTime, durationSec=:durationSec WHERE id=:id")
    suspend fun finalizeSession(id: Long, status: SessionStatus, endTime: Long, durationSec: Int)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)
}
