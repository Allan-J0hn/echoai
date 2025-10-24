package com.example.echoai.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcript_lines",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId", "chunkIndex", "offsetMs"], unique = true)]
)
data class TranscriptLine(
    @PrimaryKey
    val id: String,
    val sessionId: Long,
    val chunkIndex: Int,
    val offsetMs: Int,
    val text: String
)
