package com.example.echoai.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class SessionWithChunks(
    @Embedded val session: Session,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val chunks: List<AudioChunk>
)
