package com.example.echoai.domain

import app.cash.turbine.test
import com.example.echoai.data.local.*
import com.example.echoai.service.PauseReason
import com.example.echoai.utils.StorageGuard
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class RecordingControllerTest {

    private lateinit var mockAudioEngine: AudioEngine
    private lateinit var mockChunker: OverlapChunker
    private lateinit var mockRepository: RecordingRepository
    private lateinit var mockStateRepository: SessionStateRepository
    private lateinit var mockStorageGuard: StorageGuard

    @Before
    fun setup() {
        mockAudioEngine = mock()
        mockChunker = mock()
        mockRepository = mock()
        mockStateRepository = mock()
        mockStorageGuard = mock()
    }

    @Test
    fun `start calls createNewSession and transitions to Recording`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val controller = RecordingController(
            mockAudioEngine, mockChunker, mockRepository,
            mockStateRepository, mockStorageGuard, testDispatcher
        )
        whenever(mockRepository.createNewSession()).thenReturn(1L)

        controller.recordingStatus.test {
            assertEquals(RecordingStatus.Stopped, awaitItem())

            controller.start()
            advanceUntilIdle()

            assertEquals(RecordingStatus.Recording, awaitItem())
            verify(mockRepository).createNewSession()
            verify(mockChunker).start(eq(1L), any())
        }
    }

    @Test
    fun `stop calls finishSession and transitions to Stopped`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val controller = RecordingController(
            mockAudioEngine, mockChunker, mockRepository,
            mockStateRepository, mockStorageGuard, testDispatcher
        )
        whenever(mockRepository.createNewSession()).thenReturn(1L)
        
        controller.start()
        advanceUntilIdle()

        controller.recordingStatus.test {
            assertEquals(RecordingStatus.Recording, awaitItem())

            controller.stop()
            advanceUntilIdle()
            
            assertEquals(RecordingStatus.Stopped, awaitItem())
            verify(mockRepository).finishSession(1L)
            verify(mockAudioEngine).stop()
            verify(mockChunker).stop()
        }
    }
}
