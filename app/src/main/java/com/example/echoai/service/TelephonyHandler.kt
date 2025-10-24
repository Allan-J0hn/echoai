package com.example.echoai.service

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class TelephonyHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private var telephonyCallback: TelephonyCallback? = null
    private var phoneStateListener: PhoneStateListener? = null

    fun register(listener: (Int) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback = @RequiresApi(Build.VERSION_CODES.S)
            object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    listener(state)
                }
            }
            telephonyManager.registerTelephonyCallback(
                context.mainExecutor,
                telephonyCallback!!
            )
        } else {
            phoneStateListener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    listener(state)
                }
            }
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    fun unregister() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephonyManager.unregisterTelephonyCallback(it) }
        } else {
            phoneStateListener?.let {
                telephonyManager.listen(
                    it,
                    PhoneStateListener.LISTEN_NONE
                )
            }
        }
    }
}
