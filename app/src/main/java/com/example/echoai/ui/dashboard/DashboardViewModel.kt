package com.example.echoai.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echoai.data.local.SessionWithChunks
import com.example.echoai.domain.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(val sessions: List<SessionWithChunks>) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
open class DashboardViewModel @Inject constructor(
    private val recordingRepository: RecordingRepository
) : ViewModel() {

    open val uiState: StateFlow<DashboardUiState> = recordingRepository.observeSessions()
        .onStart { emit(emptyList()) } // ensure immediate first value
        .map<List<SessionWithChunks>, DashboardUiState>(DashboardUiState::Success)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState.Loading
        )

    fun deleteSession(id: Long) = viewModelScope.launch {
        recordingRepository.deleteSession(id) // implement in repo if missing
    }
}
