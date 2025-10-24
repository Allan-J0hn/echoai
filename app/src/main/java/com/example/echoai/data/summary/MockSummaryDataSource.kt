package com.example.echoai.data.summary

import com.example.echoai.data.local.Summary
import com.example.echoai.data.local.SummaryStatus
import javax.inject.Inject

class MockSummaryDataSource @Inject constructor() : SummaryDataSource {
    override suspend fun generateSummary(sessionId: Long, transcriptLines: List<String>): Summary {
        val nonEmptyLines = transcriptLines.map { it.trim() }.filter { it.isNotBlank() }

        val title = nonEmptyLines.firstOrNull()?.take(80) ?: "Session Summary"
        val summaryText = nonEmptyLines.take(12).joinToString("\n")

        return Summary(
            sessionId = sessionId,
            title = title,
            summaryText = summaryText,
            actionItemsJson = "[]",
            keyPointsJson = "[]",
            status = SummaryStatus.DONE,
            error = null
        )
    }
}
