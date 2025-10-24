package com.example.echoai.domain

import kotlinx.coroutines.CoroutineScope

interface AudioEngine {
    fun start(scope: CoroutineScope, onBytes: (ByteArray) -> Unit)
    fun stop()
}
