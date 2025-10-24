package com.example.echoai.data.summary

import com.example.echoai.data.local.Summary

interface SummaryDataSource {
    suspend fun generateSummary(sessionId: Long, transcriptLines: List<String>): Summary
}
