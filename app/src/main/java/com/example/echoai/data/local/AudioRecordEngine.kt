package com.example.echoai.data.local

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.echoai.domain.AudioEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioRecordEngine : AudioEngine {

    private var audioRecord: AudioRecord? = null
    private var job: Job? = null

    @SuppressLint("MissingPermission")
    override fun start(
        scope: CoroutineScope,
        onBytes: (ByteArray) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        if (job?.isActive == true) return

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT
        )
        require(bufferSize > 0) { "Could not allocate an audio recording buffer." }

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            error("Audio recorder failed to initialize.")
        }

        try {
            record.startRecording()
        } catch (throwable: Throwable) {
            record.release()
            throw throwable
        }

        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            record.release()
            error("Audio recorder did not enter recording state.")
        }

        audioRecord = record
        job = scope.launch {
            val buffer = ByteArray(bufferSize)
            try {
                while (isActive) {
                    val read = record.read(buffer, 0, buffer.size)
                    ensureActive()
                    when {
                        read > 0 -> onBytes(buffer.copyOf(read))
                        read == AudioRecord.ERROR_INVALID_OPERATION -> error("Audio recorder is in an invalid state.")
                        read == AudioRecord.ERROR_BAD_VALUE -> error("Audio recorder received an invalid buffer.")
                        read == AudioRecord.ERROR_DEAD_OBJECT -> error("Audio recorder died while recording.")
                    }
                }
            } catch (throwable: Throwable) {
                if (isActive) onError(throwable)
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
        if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord?.stop()
        }
        audioRecord?.release()
        audioRecord = null
    }

    companion object {
        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }
}
