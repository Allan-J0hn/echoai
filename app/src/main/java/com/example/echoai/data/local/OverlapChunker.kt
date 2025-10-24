package com.example.echoai.data.local

import android.content.Context
import com.example.echoai.workers.TranscriptionCoordinator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.math.min

class OverlapChunker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transcriptionCoordinator: TranscriptionCoordinator
) {
    private var onChunkClosed: ((Long, Int, String, Long) -> Unit)? = null

    fun setOnChunkClosedListener(listener: (Long, Int, String, Long) -> Unit) {
        onChunkClosed = listener
    }

    private data class AudioConfig(
        val sampleRate: Int = 16000,
        val channels: Int = 1,
        val bytesPerSample: Int = 2,
        val chunkDurationSec: Int = 30,
        val overlapDurationSec: Int = 2
    )

    private val config = AudioConfig()
    private val chunkSizeInBytes = config.sampleRate * config.bytesPerSample * config.chunkDurationSec * config.channels
    private val overlapSizeInBytes = config.sampleRate * config.bytesPerSample * config.overlapDurationSec * config.channels

    private var sessionDir: File? = null
    private var currentFile: File? = null
    private var chunkFile: RandomAccessFile? = null
    private var chunkIndex = 0
    private var bytesWritten = 0L
    private var currentSessionId: Long = 0

    private val overlapRingBuffer = ByteArray(overlapSizeInBytes)
    private var ringBufferWritePosition = 0

    fun start(sessionId: Long, nextIndex: Int = 0) {
        currentSessionId = sessionId
        val sessionPath = File(context.filesDir, "audio/$sessionId")
        sessionPath.mkdirs()
        sessionDir = sessionPath
        chunkIndex = nextIndex
        ringBufferWritePosition = 0
        createNewChunk()
    }

    fun onData(data: ByteArray) {
        var bytesToProcess = data.size
        var currentOffset = 0

        while (bytesToProcess > 0) {
            val bytesRemainingInChunk = chunkSizeInBytes - bytesWritten
            val bytesToWrite = min(bytesToProcess, bytesRemainingInChunk.toInt())

            chunkFile?.write(data, currentOffset, bytesToWrite)
            pushToRingBuffer(data, currentOffset, bytesToWrite)
            bytesWritten += bytesToWrite
            currentOffset += bytesToWrite
            bytesToProcess -= bytesToWrite

            if (bytesWritten >= chunkSizeInBytes) {
                closeChunk()
                createNewChunk()
                val overlap = readRingBuffer()
                chunkFile?.write(overlap)
                bytesWritten += overlap.size
            }
        }
    }

    fun stop() {
        closeChunk()
        sessionDir = null
        currentFile = null
    }

    private fun createNewChunk() {
        val fileName = "${currentSessionId}_${String.format("%05d", chunkIndex)}.wav"
        val file = File(sessionDir, fileName)
        currentFile = file
        chunkFile = RandomAccessFile(file, "rw")
        chunkFile?.let { writeWavHeader(it) }
        bytesWritten = 0
    }

    private fun closeChunk() {
        val fileToClose = currentFile ?: return
        val rafToClose = chunkFile ?: return

        if (rafToClose.length() <= 44) {
            rafToClose.close()
            fileToClose.delete()
            return
        }

        val totalBytes = rafToClose.length().toInt()
        val audioBytes = totalBytes - 44
        rafToClose.seek(4)
        rafToClose.writeInt(Integer.reverseBytes(totalBytes - 8))
        rafToClose.seek(40)
        rafToClose.writeInt(Integer.reverseBytes(audioBytes))
        rafToClose.close()

        val absolutePath = fileToClose.absolutePath
        onChunkClosed?.invoke(currentSessionId, chunkIndex, absolutePath, System.currentTimeMillis())
        transcriptionCoordinator.enqueueOnChunkClosed(currentSessionId, chunkIndex, absolutePath)
        chunkIndex++

        this.chunkFile = null
        this.currentFile = null
    }

    private fun writeWavHeader(file: RandomAccessFile) {
        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            put("RIFF".toByteArray())
            putInt(0)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(config.channels.toShort())
            putInt(config.sampleRate)
            putInt(config.sampleRate * config.channels * config.bytesPerSample)
            putShort((config.channels * config.bytesPerSample).toShort())
            putShort((config.bytesPerSample * 8).toShort())
            put("data".toByteArray())
            putInt(0)
        }
        file.write(header.array())
    }

    private fun pushToRingBuffer(data: ByteArray, offset: Int, length: Int) {
        for (i in 0 until length) {
            overlapRingBuffer[ringBufferWritePosition] = data[offset + i]
            ringBufferWritePosition = (ringBufferWritePosition + 1) % overlapSizeInBytes
        }
    }

    private fun readRingBuffer(): ByteArray {
        val out = ByteArray(overlapSizeInBytes)
        for (i in 0 until overlapSizeInBytes) {
            out[i] = overlapRingBuffer[(ringBufferWritePosition + i) % overlapSizeInBytes]
        }
        return out
    }
}
