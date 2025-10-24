package com.example.echoai.data.local

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.echoai.domain.AudioEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioRecordEngine : AudioEngine {

    private var audioRecord: AudioRecord? = null
    private var job: Job? = null

    @SuppressLint("MissingPermission")
    override fun start(scope: CoroutineScope, onBytes: (ByteArray) -> Unit) {
        job = scope.launch {
            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            val buffer = ByteArray(bufferSize)
            audioRecord?.startRecording()

            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    onBytes(buffer.clone())
                }
            }
        }
    }

    override fun stop() {
        job?.cancel()
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
