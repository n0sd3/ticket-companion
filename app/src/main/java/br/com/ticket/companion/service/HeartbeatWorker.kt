package br.com.ticket.companion.service

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class HeartbeatWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val heartbeat: CompanionHeartbeat
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        SyncWorker.enqueue(applicationContext)
        CompanionComponents.ensureBound(applicationContext)
        return when (heartbeat.send()) {
            SyncRun.SUCCESS -> Result.success()
            SyncRun.RETRY -> Result.retry()
            SyncRun.FAILURE -> Result.failure()
        }
    }

    companion object {
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "companion_heartbeat", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<HeartbeatWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            )
        }
    }
}
