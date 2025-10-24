package com.example.echoai.data.local

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class SessionStateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val moshi: Moshi
) {

    private val stateFile = File(context.filesDir, "state/current_session.json")

    fun save(sessionState: SessionState) {
        stateFile.parentFile?.mkdirs()
        val json = moshi.adapter(SessionState::class.java).toJson(sessionState)
        stateFile.writeText(json)
    }

    fun get(): SessionState? {
        if (!stateFile.exists()) {
            return null
        }
        val json = stateFile.readText()
        return moshi.adapter(SessionState::class.java).fromJson(json)
    }

    fun clear() {
        if (stateFile.exists()) {
            stateFile.delete()
        }
    }
}
