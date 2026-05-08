package com.example.echoai.domain

import android.os.SystemClock
import com.example.echoai.data.local.*
import com.example.echoai.utils.SilenceDetector
import com.example.echoai.utils.StorageGuard
import com.example.echoai.workers.TranscriptionCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class RecordingControllerTest {

    private lateinit var mockAudioEngine: AudioEngine
    private lateinit var mockChunker: OverlapChunker
    private lateinit var mockRepository: RecordingRepository
    private lateinit var mockStateRepository: SessionStateRepository
    private lateinit var mockTranscriptionCoordinator: TranscriptionCoordinator
    private lateinit var mockStorageGuard: StorageGuard
    private lateinit var silenceDetector: SilenceDetector

    @Before
    fun setup() {
        mockAudioEngine = mock()
        mockChunker = mock()
        mockRepository = mock()
        mockStateRepository = mock()
        mockTranscriptionCoordinator = mock()
        mockStorageGuard = mock()
        silenceDetector = SilenceDetector(16000)
        whenever(mockStateRepository.get()).thenReturn(null)
        whenever(mockStorageGuard.hasSpace(any(), any())).thenReturn(true)
    }

    @Test
    fun `start calls createNewSession and transitions to Recording`() = runTest {
        Mockito.mockStatic(SystemClock::class.java).use { mockedClock ->
            mockedClock.`when`<Long> { SystemClock.elapsedRealtime() }.thenReturn(1_000L)
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val controller = RecordingController(
                mockAudioEngine, mockChunker, mockRepository,
                mockStateRepository, mockTranscriptionCoordinator, mockStorageGuard, testDispatcher, silenceDetector
            )
            whenever(mockRepository.createNewSession()).thenReturn(1L)

            assertEquals(RecordingStatus.Stopped, controller.recordingStatus.value)

            controller.start()
            runCurrent()

            assertEquals(RecordingStatus.Recording, controller.recordingStatus.value)
            verify(mockRepository).createNewSession()
            verify(mockChunker).start(eq(1L), eq(0))

            controller.stop()
            runCurrent()
        }
    }

    @Test
    fun `stop calls finishSession and transitions to Stopped`() = runTest {
        Mockito.mockStatic(SystemClock::class.java).use { mockedClock ->
            mockedClock.`when`<Long> { SystemClock.elapsedRealtime() }.thenReturn(1_000L)
            val testDispatcher = StandardTestDispatcher(testScheduler)
            val controller = RecordingController(
                mockAudioEngine, mockChunker, mockRepository,
                mockStateRepository, mockTranscriptionCoordinator, mockStorageGuard, testDispatcher, silenceDetector
            )
            whenever(mockRepository.createNewSession()).thenReturn(1L)
            
            controller.start()
            runCurrent()

            assertEquals(RecordingStatus.Recording, controller.recordingStatus.value)

            controller.stop()
            runCurrent()
            
            assertEquals(RecordingStatus.Stopped, controller.recordingStatus.value)
            verify(mockRepository).finishSession(1L)
            verify(mockAudioEngine).stop()
            verify(mockChunker).stop()
        }
    }
}
