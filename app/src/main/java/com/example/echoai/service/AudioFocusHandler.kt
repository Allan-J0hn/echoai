package com.example.echoai.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AudioFocusHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    fun requestAudioFocus(listener: AudioManager.OnAudioFocusChangeListener): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener(listener)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            audioManager.requestAudioFocus(
                listener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            audioManager.abandonAudioFocus(null)
        }
    }
}
