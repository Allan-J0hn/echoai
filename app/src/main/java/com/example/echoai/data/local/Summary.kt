package com.example.echoai.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "summaries",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Summary(
    @PrimaryKey
    val sessionId: Long,
    val title: String = "",
    val summaryText: String = "",
    val actionItemsJson: String = "[]",
    val keyPointsJson: String = "[]",
    val status: SummaryStatus = SummaryStatus.IDLE,
    val error: String? = null
)
