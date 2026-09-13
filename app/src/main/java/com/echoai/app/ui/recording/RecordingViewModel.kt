package com.echoai.app.ui.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.echoai.app.domain.RecordingController
import com.echoai.app.domain.RecordingStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val recordingController: RecordingController
) : ViewModel() {

    val currentStatus: RecordingStatus get() = recordingController.recordingStatus.value

    val recordingStatus = recordingController.recordingStatus.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.echoai.app.domain.RecordingStatus.Stopped
    )

    val elapsedMillis = recordingController.elapsedMillis.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0L
    )
}
