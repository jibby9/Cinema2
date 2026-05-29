package com.example

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class EpgBackgroundWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("EpgBackgroundWorker", "Background EPG refresh worker started.")
        return try {
            val success = EpgRefreshManager.refreshAllEpgSources(applicationContext, force = true)
            if (success) {
                Log.d("EpgBackgroundWorker", "Background EPG refresh completed successfully. Last refresh timestamp logged.")
                Result.success()
            } else {
                Log.w("EpgBackgroundWorker", "Background EPG refresh completed with no successful sources.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("EpgBackgroundWorker", "Encountered error during background EPG refresh", e)
            Result.failure()
        }
    }
}
