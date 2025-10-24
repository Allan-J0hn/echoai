package com.example.echoai.ui.summary

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echoai.data.local.Summary
import com.example.echoai.domain.RecordingRepository
import com.example.echoai.workers.TranscriptionCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SummaryUiState {
    data object Loading : SummaryUiState
    data class Success(val summary: Summary?) : SummaryUiState
    data class Error(val message: String) : SummaryUiState
}

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repository: RecordingRepository,
    private val transcriptionCoordinator: TranscriptionCoordinator, // To start summary generation
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: -1

    val uiState: StateFlow<SummaryUiState> = repository.observeSummary(sessionId)
        .map<Summary?, SummaryUiState>(SummaryUiState::Success)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SummaryUiState.Loading
        )

    fun generateSummary() {
        if (sessionId != -1L) {
            transcriptionCoordinator.enqueueGenerateSummary(sessionId)
        }
    }
}
