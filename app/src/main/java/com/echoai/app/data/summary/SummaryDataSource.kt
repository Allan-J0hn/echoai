package com.echoai.app.data.summary

import com.echoai.app.data.local.Summary

interface SummaryDataSource {
    suspend fun generateSummary(sessionId: Long, transcriptLines: List<String>): Summary
}
