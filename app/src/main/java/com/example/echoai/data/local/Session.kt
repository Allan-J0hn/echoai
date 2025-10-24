package com.example.echoai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long?,
    val title: String,
    val status: SessionStatus,
    val durationSec: Int = 0
)
