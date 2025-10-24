package com.example.echoai.ui.common

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.echoai.data.local.SessionStatus
import com.example.echoai.data.local.SummaryStatus

@Composable
fun SessionStatusChip(status: SessionStatus) {
    AssistChip(
        onClick = { /*TODO*/ },
        label = { Text(text = status.name.replace('_', ' ')) }
    )
}

@Composable
fun SummaryStatusChip(status: SummaryStatus) {
    AssistChip(
        onClick = { /*TODO*/ },
        label = { Text(text = status.name.replace('_', ' ')) }
    )
}
