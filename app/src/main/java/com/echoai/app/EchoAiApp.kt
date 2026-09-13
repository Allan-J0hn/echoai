package com.echoai.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.echoai.app.workers.RecoverRecordingWorker
import com.echoai.app.workers.TranscriptionRequeueWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class EchoAiApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        val workManager = WorkManager.getInstance(this)

        workManager.enqueueUniqueWork(
            "RecoverRecordingWorkerImmediate",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RecoverRecordingWorker>().build()
        )

        val recoverWorkRequest = PeriodicWorkRequestBuilder<RecoverRecordingWorker>(15, TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(
            "RecoverRecordingWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            recoverWorkRequest
        )

        val requeueWorkRequest = OneTimeWorkRequestBuilder<TranscriptionRequeueWorker>().build()
        workManager.enqueue(requeueWorkRequest)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
