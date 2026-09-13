package com.echoai.app.ui.common

import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.echoai.app.data.local.SessionStatus
import com.echoai.app.data.local.SummaryStatus

@Composable
fun SessionStatusChip(status: SessionStatus) {
    AssistChip(
        onClick = { },
        label = { Text(text = status.name.replace('_', ' ')) }
    )
}

@Composable
fun SummaryStatusChip(status: SummaryStatus) {
    AssistChip(
        onClick = { },
        label = { Text(text = status.name.replace('_', ' ')) }
    )
}
