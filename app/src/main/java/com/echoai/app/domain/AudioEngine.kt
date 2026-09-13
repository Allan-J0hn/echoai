package com.echoai.app.domain

import kotlinx.coroutines.CoroutineScope

interface AudioEngine {
    fun start(
        scope: CoroutineScope,
        onBytes: (ByteArray) -> Unit,
        onError: (Throwable) -> Unit = {}
    )
    fun stop()
}
