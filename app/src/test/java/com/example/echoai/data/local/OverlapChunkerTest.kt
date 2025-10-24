package com.example.echoai.data.local

import android.content.Context
import com.example.echoai.workers.TranscriptionCoordinator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.*
import java.io.File

@RunWith(MockitoJUnitRunner::class)
class OverlapChunkerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockTranscriptionCoordinator: TranscriptionCoordinator

    private lateinit var overlapChunker: OverlapChunker
    private val fakeSessionId = 123L

    @Before
    fun setup() {
        val tempDir = tempFolder.newFolder()
        whenever(mockContext.filesDir).thenReturn(tempDir)
        
        overlapChunker = OverlapChunker(mockContext, mockTranscriptionCoordinator)
    }

    @Test
    fun `start creates session directory`() {
        overlapChunker.start(fakeSessionId)
        val sessionDir = File(mockContext.filesDir, "audio/$fakeSessionId")
        assert(sessionDir.exists() && sessionDir.isDirectory)
    }

    @Test
    fun `stop finalizes last chunk and cleans up`() {
        val onChunkClosedCallback = mock<(Long, Int, String, Long) -> Unit>()
        overlapChunker.setOnChunkClosedListener(onChunkClosedCallback)
        
        overlapChunker.start(fakeSessionId)
        val testData = ByteArray(1024)
        overlapChunker.onData(testData)
        
        overlapChunker.stop()

        verify(onChunkClosedCallback).invoke(eq(fakeSessionId), eq(0), any(), any())
        verify(mockTranscriptionCoordinator).enqueueOnChunkClosed(eq(fakeSessionId), eq(0), any())
    }

    @Test
    fun `onData does not create empty chunks`() {
        val onChunkClosedCallback = mock<(Long, Int, String, Long) -> Unit>()
        overlapChunker.setOnChunkClosedListener(onChunkClosedCallback)
        
        overlapChunker.start(fakeSessionId)
        overlapChunker.stop()

        verify(onChunkClosedCallback, never()).invoke(any(), any(), any(), any())
        verify(mockTranscriptionCoordinator, never()).enqueueOnChunkClosed(any(), any(), any())
    }
}
