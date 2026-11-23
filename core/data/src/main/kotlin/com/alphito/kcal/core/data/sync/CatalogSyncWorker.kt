package com.alphito.kcal.core.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.alphito.kcal.core.data.repository.CatalogRepository
import com.alphito.kcal.core.data.repository.CatalogSyncResult
import java.util.concurrent.TimeUnit

/** Pulls the remote food catalogue in the background (never on the main thread / cold-start path). */
class CatalogSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val catalog: CatalogRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = when (catalog.sync()) {
        is CatalogSyncResult.Failed -> if (runAttemptCount < 3) Result.retry() else Result.failure()
        else -> Result.success()
    }

    companion object {
        private const val ONCE = "catalog-sync-once"
        private const val DAILY = "catalog-sync-daily"
        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** On every launch (deduplicated) + once a day while the app is installed. */
        fun schedule(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.enqueueUniqueWork(
                ONCE, ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<CatalogSyncWorker>().setConstraints(online).build(),
            )
            wm.enqueueUniquePeriodicWork(
                DAILY, ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<CatalogSyncWorker>(1, TimeUnit.DAYS).setConstraints(online).build(),
            )
        }
    }
}
