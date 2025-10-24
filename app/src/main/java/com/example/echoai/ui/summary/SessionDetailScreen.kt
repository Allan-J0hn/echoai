package com.example.echoai.ui.summary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SessionDetailScreen(
    viewModel: TranscriptViewModel = hiltViewModel()
) {
    val transcript by viewModel.transcript.collectAsState()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (transcript.isEmpty()) {
            Text(text = "No transcript yet")
        } else {
            LazyColumn(
                modifier = Modifier.padding(16.dp)
            ) {
                items(transcript) { line ->
                    Text(text = line.text)
                }
            }
        }
    }
}
