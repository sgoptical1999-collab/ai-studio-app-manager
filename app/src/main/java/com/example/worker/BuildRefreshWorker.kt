package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.AppManagerApplication
import com.example.R
import java.util.concurrent.TimeUnit

class BuildRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? AppManagerApplication ?: return Result.success()
        val settings = app.settingsDataStore.getSnapshot()
        if (!settings.autoRefreshEnabled) return Result.success()

        val refreshedCount = app.repository.refreshAllConnectedApps().getOrDefault(0)
        if (refreshedCount > 0 && settings.notificationsEnabled) {
            NotificationHelper.postBuildNotification(
                context = applicationContext,
                title = "GitHub Builds Refreshed",
                message = "Checked latest Actions status & APK artifacts for $refreshedCount app(s)."
            )
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "ai_studio_periodic_github_refresh"

        fun scheduleOrCancel(
            context: Context,
            enabled: Boolean,
            intervalHours: Int,
            wifiOnly: Boolean
        ) {
            val wm = WorkManager.getInstance(context)
            if (!enabled) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<BuildRefreshWorker>(
                intervalHours.toLong().coerceAtLeast(1L),
                TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            wm.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}

object NotificationHelper {
    const val CHANNEL_ID = "ai_studio_app_manager_builds"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Build & APK Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for GitHub Actions build updates and APK downloads"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    fun postBuildNotification(context: Context, title: String, message: String) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        runCatching {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(context).notify(
                (System.currentTimeMillis() % 100_000L).toInt(),
                notification
            )
        }
    }
}
