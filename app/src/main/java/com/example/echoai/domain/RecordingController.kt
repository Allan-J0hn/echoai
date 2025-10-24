package com.example.echoai.domain

import android.os.SystemClock
import com.example.echoai.data.local.*
import com.example.echoai.di.IoDispatcher
import com.example.echoai.service.PauseReason
import com.example.echoai.utils.SilenceDetector
import com.example.echoai.utils.SilenceEvent
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
            }
        }
    }

    fun start() {
        scope.launch {
            gate.withLock {
                if (_recordingStatus.value is RecordingStatus.Recording) return@withLock

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

                chunker.start(newSessionId, 0)
                startTimer()
                silenceDetector.reset() // Reset silence detector on new recording
                try {
                    audioEngine.start(scope) { data ->
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
                } catch (e: Exception) {
                    Timber.e(e, "Audio engine failed to start")
                    _recordingStatus.value = RecordingStatus.Error("Audio engine failed")
                    audioEngine.stop()
                    chunker.stop()
                    timerJob?.cancel()
                }
            }
        }
    }

    fun stop() {
        scope.launch {
            gate.withLock {
                audioEngine.stop()
                chunker.stop()
                timerJob?.cancel()
                currentSessionId?.let {
                    repository.finishSession(it)
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
            if (_recordingStatus.value is RecordingStatus.Paused) {
                lastResumedRealtimeMs = SystemClock.elapsedRealtime()
                _recordingStatus.value = RecordingStatus.Recording
                currentSessionId?.let { sessionId ->
                    val session = repository.getSessionWithChunks(sessionId)
                    val lastChunkIndex = session?.chunks?.lastOrNull()?.index ?: 0
                    sessionStateRepository.save(SessionState(
                        sessionId,
                        session?.session?.startTime ?: System.currentTimeMillis(),
                        lastChunkIndex,
                        SessionStatus.RECORDING,
                        startRealtimeMs,
                        accumulatedElapsedMs,
                        lastResumedRealtimeMs
                    ))
                }
                startTimer()
                silenceDetector.reset() // Reset silence detector on resume
                try {
                    audioEngine.start(scope) { data -> 
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
                } catch (e: Exception) {
                    Timber.e(e, "Audio engine failed to resume")
                    _recordingStatus.value = RecordingStatus.Error("Audio engine failed")
                    audioEngine.stop()
                    chunker.stop()
                    timerJob?.cancel()
                }
                Timber.d("Controller State (Resume): status=%s now=%d start=%d lastResumed=%s acc=%d elapsed=%d",
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
}
