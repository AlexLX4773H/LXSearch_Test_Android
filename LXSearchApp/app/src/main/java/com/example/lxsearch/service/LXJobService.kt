package com.example.lxsearch.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.example.lxsearch.MainActivity
import com.example.lxsearch.core.CreateFileListV1
import com.example.lxsearch.core.CreateFileListV2
import com.example.lxsearch.core.MoveFromSource
import com.example.lxsearch.core.MoveToTemp
import com.example.lxsearch.core.NameCircleRelation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LXJobService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "LXSearch:JobExecutionWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    private var activeExecutionJob: kotlinx.coroutines.Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            activeExecutionJob?.cancel()
            return START_NOT_STICKY
        }

        val job = LXJobManager.pendingJob
        if (job == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        LXJobManager.pendingJob = null

        // Start foreground notification immediately
        val initialNotification = NotificationHelper.buildProgressNotification(
            this,
            job.title,
            "Starting execution..."
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ID_PROGRESS,
                initialNotification,
                serviceType
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID_PROGRESS, initialNotification)
        }

        wakeLock?.acquire(30 * 60 * 1000L) // 30 minutes max safety limit

        activeExecutionJob = serviceScope.launch {
            executeJob(job)
        }

        return START_NOT_STICKY
    }

    private fun executeJob(job: LXJob) {
        var lastNotificationTime = 0L

        fun updateProgress(text: String, count: Int = 0) {
            LXJobManager.updateProgress(text, count)
            LXJobManager.addLog(text)

            val now = System.currentTimeMillis()
            if (now - lastNotificationTime >= 800L) {
                lastNotificationTime = now
                val notification = NotificationHelper.buildProgressNotification(
                    this@LXJobService,
                    job.title,
                    text
                )
                notificationManager.notify(NotificationHelper.NOTIFICATION_ID_PROGRESS, notification)
            }
        }

        try {
            val outputDir = MainActivity.getOutputDir(this)
            val inputDir = MainActivity.getInputDir(this)

            var summary = ""
            var resultData: Any? = null
            val isCancelledCheck = { LXJobManager.isCancelled || !(activeExecutionJob?.isActive ?: true) }

            when (job) {
                is LXJob.CreateFileList -> {
                    val count = if (job.isV2) {
                        CreateFileListV2.run(outputDir, inputDir, isCancelled = isCancelledCheck) { progress ->
                            updateProgress("${progress.current}: ${progress.name}", progress.current)
                        }
                    } else {
                        CreateFileListV1.run(outputDir, inputDir, isCancelled = isCancelledCheck) { progress ->
                            updateProgress("${progress.current}: ${progress.name}", progress.current)
                        }
                    }
                    summary = "Processed $count items successfully."
                    resultData = count
                    val key = if (job.isV2) com.example.lxsearch.data.JobHistoryManager.JobKey.CREATE_FILE_LIST_V2 else com.example.lxsearch.data.JobHistoryManager.JobKey.CREATE_FILE_LIST_V1
                    com.example.lxsearch.data.JobHistoryManager.recordLastRun(this@LXJobService, key)
                }
                is LXJob.NameCircle -> {
                    val r = NameCircleRelation.run(outputDir, inputDir, isCancelled = isCancelledCheck) { progress ->
                        updateProgress(progress.message)
                    }
                    summary = "Processed ${r.seriesCount} series (${r.duplicateCount} duplicates found)."
                    resultData = r
                    com.example.lxsearch.data.JobHistoryManager.recordLastRun(this@LXJobService, com.example.lxsearch.data.JobHistoryManager.JobKey.NAME_CIRCLE)
                }
                is LXJob.MoveToTempScan -> {
                    updateProgress("Scanning for empty and incomplete folders...")
                    val result = MoveToTemp.scan()
                    summary = "Found ${result.moveItems.size} items to move, ${result.emptyFolders.size} empty folders."
                    resultData = result
                }
                is LXJob.MoveToTempMove -> {
                    updateProgress("Moving ${job.items.size} items to temp...")
                    val logs = MoveToTemp.executeMove(job.items)
                    logs.forEach { LXJobManager.addLog(it) }
                    summary = "Moved ${job.items.size} items to temp destinations."
                    resultData = logs
                }
                is LXJob.MoveToTempRemove -> {
                    updateProgress("Removing ${job.folders.size} empty folders...")
                    val logs = MoveToTemp.executeRemove(job.folders)
                    logs.forEach { LXJobManager.addLog(it) }
                    summary = "Removed ${job.folders.size} empty folders."
                    resultData = logs
                }
                is LXJob.MoveFromSourceScan -> {
                    updateProgress("Scanning source releases...")
                    val result = MoveFromSource.scan(outputDir, inputDir)
                    summary = "Matched ${result.items.size} items to series folders."
                    resultData = result
                }
                is LXJob.MoveFromSourceMove -> {
                    updateProgress("Moving ${job.items.size} source items to destinations...")
                    val logs = MoveFromSource.executeMove(job.items)
                    logs.forEach { LXJobManager.addLog(it) }
                    summary = "Moved ${job.items.size} items to destination folders."
                    resultData = logs
                }
            }

            LXJobManager.completeJob(summary, resultData)
            NotificationHelper.showCompletionNotification(
                this@LXJobService,
                job.title,
                summary,
                isSuccess = true
            )
        } catch (ce: java.util.concurrent.CancellationException) {
            LXJobManager.notifyJobCancelled(job)
            NotificationHelper.showCompletionNotification(
                this@LXJobService,
                job.title,
                "Job was aborted by user.",
                isSuccess = false
            )
        } catch (t: Throwable) {
            val errorMsg = t.message ?: "An unexpected error occurred."
            LXJobManager.failJob(errorMsg)
            NotificationHelper.showCompletionNotification(
                this@LXJobService,
                job.title,
                errorMsg,
                isSuccess = false
            )
        } finally {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            ServiceCompat.stopForeground(this@LXJobService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_CANCEL = "com.example.lxsearch.service.ACTION_CANCEL"
    }
}
