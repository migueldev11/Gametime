package com.valerio.gametime

import android.app.Application
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

class GameTimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        scheduleSync(this)
    }
}

class SyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        try {
            UsageSync.sync(applicationContext)
        } catch (e: Exception) {
            // tenta de novo no próximo ciclo
        }
        return Result.success()
    }
}

fun scheduleSync(ctx: Context) {
    val req = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("gametime-sync", ExistingPeriodicWorkPolicy.KEEP, req)
}
