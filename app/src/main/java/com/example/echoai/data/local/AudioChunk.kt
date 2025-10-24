package com.example.echoai.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_chunks",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AudioChunk(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val index: Int,
    val filePath: String,
    val durationSec: Int,
    val uploaded: Boolean = false,
    val transcribed: Boolean = false
)
