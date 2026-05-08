package com.example.echoai.data.summary

import com.example.echoai.data.local.Summary
import com.example.echoai.data.local.SummaryStatus
import javax.inject.Inject

class MockSummaryDataSource @Inject constructor() : SummaryDataSource {
    override suspend fun generateSummary(sessionId: Long, transcriptLines: List<String>): Summary {
        val nonEmptyLines = transcriptLines.map { it.trim() }.filter { it.isNotBlank() }

        val title = nonEmptyLines.firstOrNull()?.toTitle()?.take(80) ?: "Session Summary"
        val keyPoints = nonEmptyLines
            .take(4)
            .mapIndexed { index, line -> "Point ${index + 1}: ${line.take(120)}" }
        val summaryText = if (nonEmptyLines.isEmpty()) {
            "No transcript is available for this session yet."
        } else {
            "This demo summary highlights ${nonEmptyLines.size} captured transcript lines. " +
                nonEmptyLines.take(3).joinToString(" ")
        }

        return Summary(
            sessionId = sessionId,
            title = title,
            summaryText = summaryText,
            actionItemsJson = "[]",
            keyPointsJson = keyPoints.toJsonArray(),
            status = SummaryStatus.DONE,
            error = null
        )
    }

    private fun String.toTitle(): String {
        return replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }.trimEnd('.', ',', ';', ':')
    }

    private fun List<String>.toJsonArray(): String {
        return joinToString(prefix = "[", postfix = "]") { "\"${it.escapeJson()}\"" }
    }

    private fun String.escapeJson(): String {
        return buildString {
            this@escapeJson.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }
    }
}
