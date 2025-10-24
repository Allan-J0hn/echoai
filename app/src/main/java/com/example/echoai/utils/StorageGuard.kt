package com.example.echoai.utils

import android.content.Context
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StorageGuard @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun hasSpace(bytesNeeded: Long, minReserve: Long = 50L * 1024 * 1024): Boolean {
        val stat = StatFs(context.filesDir.absolutePath)
        val availableBytes = stat.availableBytes
        return availableBytes > bytesNeeded + minReserve
    }
}
