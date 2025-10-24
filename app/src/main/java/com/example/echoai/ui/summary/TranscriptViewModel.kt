package com.example.echoai.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echoai.domain.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId = savedStateHandle.get<Long>("sessionId") ?: -1

    val transcript = recordingRepository.observeTranscript(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
