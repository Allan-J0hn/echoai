package com.example.echoai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TranscriptLineDao {
    @Insert
    suspend fun insert(transcriptLine: TranscriptLine)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lines: List<TranscriptLine>)

    @Query("SELECT * FROM transcript_lines WHERE sessionId = :sessionId ORDER BY chunkIndex ASC, offsetMs ASC")
    fun observeTranscript(sessionId: Long): Flow<List<TranscriptLine>>

    @Query("SELECT text FROM transcript_lines WHERE sessionId = :sessionId ORDER BY chunkIndex ASC, offsetMs ASC")
    suspend fun getTranscriptLinesForSession(sessionId: Long): List<String>
}
