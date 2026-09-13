package com.echoai.app.domain

import android.os.SystemClock
import com.echoai.app.data.local.*
import com.echoai.app.di.IoDispatcher
import com.echoai.app.service.PauseReason
import com.echoai.app.utils.SilenceDetector
import com.echoai.app.utils.SilenceEvent
import com.echoai.app.utils.StorageGuard
import com.echoai.app.workers.TranscriptionCoordinator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingController @Inject constructor(
    private val audioEngine: AudioEngine,
    private val chunker: OverlapChunker,
    private val repository: RecordingRepository,
    private val sessionStateRepository: SessionStateRepository,
    private val transcriptionCoordinator: TranscriptionCoordinator,
    private val storageGuard: StorageGuard,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val silenceDetector: SilenceDetector
) {

    private val _elapsedMillis = MutableStateFlow(0L)
    val elapsedMillis = _elapsedMillis.asStateFlow()

    private val _recordingStatus = MutableStateFlow<RecordingStatus>(RecordingStatus.Stopped)
    val recordingStatus = _recordingStatus.asStateFlow()

    private val job = SupervisorJob()
    private val scope = CoroutineScope(ioDispatcher + job)
    private val gate = Mutex()

    private var timerJob: Job? = null
    private var currentSessionId: Long? = null
    private var audioCaptureRunning = false
    private var chunkerRunning = false

    // New state variables for robust timekeeping
    private var startRealtimeMs: Long = 0L
    private var accumulatedElapsedMs: Long = 0L
    private var lastResumedRealtimeMs: Long? = null

    init {
        // Restore state on initialization for process death recovery
        sessionStateRepository.get()?.let { state ->
            currentSessionId = state.sessionId
            startRealtimeMs = state.startRealtimeMs
            accumulatedElapsedMs = state.accumulatedElapsedMs
            lastResumedRealtimeMs = state.lastResumedRealtimeMs

            when (state.status) {
                SessionStatus.RECORDING -> {
                    _recordingStatus.value = RecordingStatus.Recording
                    startTimer()
                }
                SessionStatus.PAUSED -> {
                    _recordingStatus.value = RecordingStatus.Paused(PauseReason.PROCESS_RESTART)
                    _elapsedMillis.value = accumulatedElapsedMs
                }
                else -> {
                    // If status is IDLE or STOPPED, it implies a clean state or no active session
                    currentSessionId = null
                    startRealtimeMs = 0L
                    accumulatedElapsedMs = 0L
                    lastResumedRealtimeMs = null
                    _elapsedMillis.value = 0L
                    _recordingStatus.value = RecordingStatus.Stopped
                }
            }
            Timber.d("Controller State Restored: status=%s now=%d start=%d lastResumed=%s acc=%d elapsed=%d",
                _recordingStatus.value.javaClass.simpleName,
                SystemClock.elapsedRealtime(),
                startRealtimeMs,
                lastResumedRealtimeMs,
                accumulatedElapsedMs,
                _elapsedMillis.value
            )
            silenceDetector.reset()
        } ?: silenceDetector.reset()

        chunker.setOnChunkClosedListener { sessionId, chunkIndex, filePath, _ ->
            scope.launch {
                repository.addAudioChunk(
                    AudioChunk(sessionId = sessionId, index = chunkIndex, filePath = filePath, durationSec = 30)
                )
                transcriptionCoordinator.enqueueOnChunkClosed(sessionId, chunkIndex, filePath)
            }
        }
    }

    fun start() {
        scope.launch {
            gate.withLock {
                if (_recordingStatus.value is RecordingStatus.Recording || _recordingStatus.value is RecordingStatus.Warning) {
                    recoverCaptureIfNeededLocked()
                    return@withLock
                }
                if (_recordingStatus.value is RecordingStatus.Paused) {
                    resumeLocked()
                    return@withLock
                }
                if (!hasRecordingSpaceLocked()) return@withLock

                val newSessionId = repository.createNewSession()
                currentSessionId = newSessionId
                
                startRealtimeMs = SystemClock.elapsedRealtime()
                accumulatedElapsedMs = 0L
                lastResumedRealtimeMs = SystemClock.elapsedRealtime()

                sessionStateRepository.save(SessionState(
                    newSessionId,
                    System.currentTimeMillis(), // Using System.currentTimeMillis() for session.startedAt
                    0,
                    SessionStatus.RECORDING,
                    startRealtimeMs,
                    accumulatedElapsedMs,
                    lastResumedRealtimeMs
                ))
                _recordingStatus.value = RecordingStatus.Recording
                Timber.d("Controller State (Start): status=%s now=%d start=%d lastResumed=%s acc=%d elapsed=%d",
                    _recordingStatus.value.javaClass.simpleName,
                    SystemClock.elapsedRealtime(),
                    startRealtimeMs,
                    lastResumedRealtimeMs,
                    accumulatedElapsedMs,
                    _elapsedMillis.value
                )

                startCaptureLocked(newSessionId, nextChunkIndex = 0, failureAction = "start")
            }
        }
    }

    fun stop() {
        scope.launch {
            gate.withLock {
                val sessionId = currentSessionId
                audioEngine.stop()
                audioCaptureRunning = false
                if (chunkerRunning) {
                    chunker.stop()
                    chunkerRunning = false
                }
                timerJob?.cancel()
                sessionId?.let {
                    repository.finishSession(it)
                    enqueueSummaryIfReady(it)
                }
                _recordingStatus.value = RecordingStatus.Stopped
                _elapsedMillis.value = 0L
                currentSessionId = null
                sessionStateRepository.clear()
                silenceDetector.reset()
                
                // Reset timekeeping variables
                startRealtimeMs = 0L
                accumulatedElapsedMs = 0L
                lastResumedRealtimeMs = null

                Timber.d("Controller State (Stop): status=%s now=%d start=%d lastResumed=%s acc=%d elapsed=%d",
                    _recordingStatus.value.javaClass.simpleName,
                    SystemClock.elapsedRealtime(),
                    startRealtimeMs,
                    lastResumedRealtimeMs,
                    accumulatedElapsedMs,
                    _elapsedMillis.value
                )
            }
        }
    }

    fun pause(reason: PauseReason) {
        scope.launch {
            gate.withLock {
                if (_recordingStatus.value is RecordingStatus.Recording || _recordingStatus.value is RecordingStatus.Warning) {
                    val now = SystemClock.elapsedRealtime()
                    lastResumedRealtimeMs?.let { accumulatedElapsedMs += (now - it) }
                    lastResumedRealtimeMs = null

                    _recordingStatus.value = RecordingStatus.Paused(reason)
                    currentSessionId?.let { sessionId ->
                        val session = repository.getSessionWithChunks(sessionId)
                        val lastChunkIndex = session?.chunks?.lastOrNull()?.index ?: 0
                        sessionStateRepository.save(SessionState(
                            sessionId,
                            session?.session?.startTime ?: System.currentTimeMillis(),
                            lastChunkIndex,
                            SessionStatus.PAUSED,
                            startRealtimeMs,
                            accumulatedElapsedMs,
                            lastResumedRealtimeMs
                        ))
                    }
                    audioEngine.stop()
                    audioCaptureRunning = false
                    timerJob?.cancel()
                    Timber.d("Controller State (Pause): status=%s now=%d start=%d lastResumed=%s acc=%d elapsed=%d",
                        _recordingStatus.value.javaClass.simpleName,
                        SystemClock.elapsedRealtime(),
                        startRealtimeMs,
                        lastResumedRealtimeMs,
                        accumulatedElapsedMs,
                        _elapsedMillis.value
                    )
                }
            }
        }
    }

    suspend fun resume() {
        gate.withLock {
            resumeLocked()
        }
    }

    suspend fun resumeIfPausedFor(reason: PauseReason) {
        gate.withLock {
            val currentStatus = _recordingStatus.value
            if (currentStatus is RecordingStatus.Paused && currentStatus.reason == reason) {
                resumeLocked()
            }
        }
    }

    private suspend fun resumeLocked() {
        if (_recordingStatus.value !is RecordingStatus.Paused) return
        if (!hasRecordingSpaceLocked()) return

        val sessionId = currentSessionId ?: return
        val session = repository.getSessionWithChunks(sessionId)
        val nextChunkIndex = nextChunkIndexForSession(sessionId)

        lastResumedRealtimeMs = SystemClock.elapsedRealtime()
        _recordingStatus.value = RecordingStatus.Recording
        sessionStateRepository.save(SessionState(
            sessionId,
            session?.session?.startTime ?: System.currentTimeMillis(),
            nextChunkIndex,
            SessionStatus.RECORDING,
            startRealtimeMs,
            accumulatedElapsedMs,
            lastResumedRealtimeMs
        ))

        startCaptureLocked(sessionId, nextChunkIndex, failureAction = "resume")
        Timber.d("Controller State (Resume): status=%s now=%d start=%d lastResumed=%s acc=%d elapsed=%d",
            _recordingStatus.value.javaClass.simpleName,
            SystemClock.elapsedRealtime(),
            startRealtimeMs,
            lastResumedRealtimeMs,
            accumulatedElapsedMs,
            _elapsedMillis.value
        )
    }

    private suspend fun recoverCaptureIfNeededLocked() {
        if (audioCaptureRunning) return
        if (!hasRecordingSpaceLocked()) return

        val sessionId = currentSessionId ?: return
        val session = repository.getSessionWithChunks(sessionId)
        val nextChunkIndex = nextChunkIndexForSession(sessionId)

        if (lastResumedRealtimeMs == null) {
            lastResumedRealtimeMs = SystemClock.elapsedRealtime()
        }
        sessionStateRepository.save(SessionState(
            sessionId,
            session?.session?.startTime ?: System.currentTimeMillis(),
            nextChunkIndex,
            SessionStatus.RECORDING,
            startRealtimeMs,
            accumulatedElapsedMs,
            lastResumedRealtimeMs
        ))

        startCaptureLocked(sessionId, nextChunkIndex, failureAction = "recover")
        Timber.d("Controller State (Recover): status=%s now=%d start=%d lastResumed=%s acc=%d elapsed=%d",
            _recordingStatus.value.javaClass.simpleName,
            SystemClock.elapsedRealtime(),
            startRealtimeMs,
            lastResumedRealtimeMs,
            accumulatedElapsedMs,
            _elapsedMillis.value
        )
    }

    private suspend fun nextChunkIndexForSession(sessionId: Long): Int {
        val dbNextIndex = (repository.getSessionWithChunks(sessionId)?.chunks?.maxOfOrNull { it.index } ?: -1) + 1
        val stateNextIndex = sessionStateRepository.get()?.lastChunkIndex ?: 0
        return maxOf(dbNextIndex, stateNextIndex, 0)
    }

    private fun startCaptureLocked(sessionId: Long, nextChunkIndex: Int, failureAction: String) {
        if (!chunkerRunning) {
            chunker.start(sessionId, nextChunkIndex)
            chunkerRunning = true
        }
        startTimer()
        silenceDetector.reset()
        try {
            audioEngine.start(
                scope = scope,
                onBytes = { data -> handleAudioData(data) },
                onError = { throwable -> handleAudioEngineError(throwable) }
            )
            audioCaptureRunning = true
        } catch (e: Exception) {
            Timber.e(e, "Audio engine failed to %s", failureAction)
            markAudioCaptureFailedLocked()
        }
    }

    private fun handleAudioEngineError(throwable: Throwable) {
        scope.launch {
            gate.withLock {
                Timber.e(throwable, "Audio engine failed while recording")
                markAudioCaptureFailedLocked()
            }
        }
    }

    private fun markAudioCaptureFailedLocked() {
        _recordingStatus.value = RecordingStatus.Error("Audio engine failed")
        audioEngine.stop()
        audioCaptureRunning = false
        if (chunkerRunning) {
            chunker.stop()
            chunkerRunning = false
        }
        timerJob?.cancel()
    }

    private fun handleAudioData(data: ByteArray) {
        chunker.onData(data)
        when (silenceDetector.onData(data)) {
            SilenceEvent.SilentFor10s -> {
                if (_recordingStatus.value !is RecordingStatus.Warning) {
                    _recordingStatus.value = RecordingStatus.Warning("No audio detected – Check microphone.")
                    Timber.w("SilenceDetector: No audio detected – Check microphone.")
                }
            }
            SilenceEvent.SoundResumed -> {
                if (_recordingStatus.value is RecordingStatus.Warning) {
                    _recordingStatus.value = RecordingStatus.Recording
                    Timber.d("SilenceDetector: Sound resumed, clearing warning.")
                }
            }
            SilenceEvent.NoChange -> { /* do nothing */ }
        }
    }

    private suspend fun enqueueSummaryIfReady(sessionId: Long) {
        val session = repository.getSessionWithChunks(sessionId) ?: return
        if (session.session.status == SessionStatus.STOPPED &&
            session.chunks.isNotEmpty() &&
            session.chunks.all { it.transcribed }
        ) {
            transcriptionCoordinator.enqueueGenerateSummary(sessionId)
        }
    }

    private fun hasRecordingSpaceLocked(): Boolean {
        if (storageGuard.hasSpace(MIN_RECORDING_BYTES_NEEDED)) return true

        _recordingStatus.value = RecordingStatus.Error("Not enough storage to record.")
        timerJob?.cancel()
        return false
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (isActive) {
                val now = SystemClock.elapsedRealtime()
                _elapsedMillis.value = accumulatedElapsedMs + (lastResumedRealtimeMs?.let { now - it } ?: 0L)
                delay(100)
            }
        }
    }

    private companion object {
        private const val MIN_RECORDING_BYTES_NEEDED = 1_000_000L
    }
}
