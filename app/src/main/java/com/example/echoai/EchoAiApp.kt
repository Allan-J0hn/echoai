package com.example.echoai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.echoai.workers.RecoverRecordingWorker
import com.example.echoai.workers.TranscriptionRequeueWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class EchoAiApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        val recoverWorkRequest = PeriodicWorkRequestBuilder<RecoverRecordingWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RecoverRecordingWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            recoverWorkRequest
        )

        val requeueWorkRequest = OneTimeWorkRequestBuilder<TranscriptionRequeueWorker>().build()
        WorkManager.getInstance(this).enqueue(requeueWorkRequest)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
