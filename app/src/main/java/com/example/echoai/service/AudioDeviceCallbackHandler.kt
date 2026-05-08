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

    fun register(callback: (Boolean) -> Unit) {
        audioDeviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                if (addedDevices?.any { it.isSource } == true) {
                    callback(true)
                }
            }

            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                if (removedDevices?.any { it.isSource } == true) {
                    callback(hasInputDevice())
                }
            }
        }
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        callback(hasInputDevice())
    }

    fun unregister() {
        audioDeviceCallback?.let { audioManager.unregisterAudioDeviceCallback(it) }
        audioDeviceCallback = null
    }

    fun hasInputDevice(): Boolean {
        return audioManager
            .getDevices(AudioManager.GET_DEVICES_INPUTS)
            .any { it.isSource }
    }
}
