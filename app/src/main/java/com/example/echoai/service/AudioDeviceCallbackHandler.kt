package com.example.echoai.service

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AudioDeviceCallbackHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioDeviceCallback: AudioDeviceCallback? = null

    fun register(callback: (AudioDeviceInfo?) -> Unit) {
        audioDeviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                callback(addedDevices?.firstOrNull { it.isSource })
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                callback(null)
            }
        }
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
    }

    fun unregister() {
        audioDeviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
    }
}
