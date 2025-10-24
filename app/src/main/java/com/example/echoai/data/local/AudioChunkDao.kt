package com.example.echoai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface AudioChunkDao {
    @Insert
    suspend fun insert(audioChunk: AudioChunk)

    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId AND `index` = :index")
    suspend fun getChunk(sessionId: Long, index: Int): AudioChunk?

    @Update
    suspend fun updateChunk(audioChunk: AudioChunk)

    @Query("UPDATE audio_chunks SET uploaded = :uploaded WHERE sessionId = :sessionId AND `index` = :index")
    suspend fun markChunkUploaded(sessionId: Long, index: Int, uploaded: Boolean)

    @Query("UPDATE audio_chunks SET transcribed = :transcribed WHERE sessionId = :sessionId AND `index` = :index")
    suspend fun markChunkTranscribed(sessionId: Long, index: Int, transcribed: Boolean)

    @Query("SELECT * FROM audio_chunks WHERE sessionId = :sessionId AND transcribed = 0 ORDER BY `index` ASC")
    fun findNextUntranscribedChunks(sessionId: Long): List<AudioChunk>
}
