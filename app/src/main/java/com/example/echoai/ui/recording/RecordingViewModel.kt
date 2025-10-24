package com.example.echoai.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echoai.domain.RecordingController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val recordingController: RecordingController
) : ViewModel() {

    val recordingStatus = recordingController.recordingStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.echoai.domain.RecordingStatus.Stopped
    )

    val elapsedMillis = recordingController.elapsedMillis.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )
}
