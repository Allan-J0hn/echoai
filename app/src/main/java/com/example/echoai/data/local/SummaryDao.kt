package com.example.echoai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SummaryDao {
    @Upsert
    suspend fun upsert(summary: Summary)

    @Query("SELECT * FROM summaries WHERE sessionId = :sessionId")
    fun observeSummary(sessionId: Long): Flow<Summary?>
}
