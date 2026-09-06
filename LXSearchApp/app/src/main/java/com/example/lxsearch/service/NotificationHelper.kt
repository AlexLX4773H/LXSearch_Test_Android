package com.example.lxsearch.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.lxsearch.MainActivity
import com.example.lxsearch.R

object NotificationHelper {

    const val CHANNEL_ID_PROGRESS = "lx_job_progress_channel"
    const val CHANNEL_ID_COMPLETION = "lx_job_completion_channel"

    const val NOTIFICATION_ID_PROGRESS = 1001
    const val NOTIFICATION_ID_COMPLETION = 1002

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Progress channel (silent ongoing notifications)
            val progressChannel = NotificationChannel(
                CHANNEL_ID_PROGRESS,
                "Job Progress",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notifications while jobs are actively processing in the background"
                setShowBadge(false)
            }

            // Completion channel (audible / heads up alerts)
            val completionChannel = NotificationChannel(
                CHANNEL_ID_COMPLETION,
                "Job Completion",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when a background job completes or fails"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(progressChannel)
            notificationManager.createNotificationChannel(completionChannel)
        }
    }

    private fun getContentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAVIGATE_RUNNING_JOB", true)
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildProgressNotification(
        context: Context,
        jobTitle: String,
        progressText: String
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS)
            .setSmallIcon(R.drawable.ic_lx_logo)
            .setContentTitle("LX Search: $jobTitle")
            .setContentText(progressText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(progressText))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(getContentIntent(context))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showCompletionNotification(
        context: Context,
        jobTitle: String,
        summaryText: String,
        isSuccess: Boolean
    ) {
        val title = if (isSuccess) "✓ $jobTitle Completed" else "⚠ $jobTitle Failed"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_COMPLETION)
            .setSmallIcon(R.drawable.ic_lx_logo)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summaryText))
            .setAutoCancel(true)
            .setContentIntent(getContentIntent(context))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_COMPLETION, notification)
    }
}
