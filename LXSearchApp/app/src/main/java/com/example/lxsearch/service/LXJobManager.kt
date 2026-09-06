package com.example.lxsearch.service

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.lxsearch.core.MoveFromSource
import com.example.lxsearch.core.MoveToTemp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

sealed interface LXJob {
    val title: String

    data class CreateFileList(val isV2: Boolean) : LXJob {
        override val title: String = if (isV2) "Create File List (V2)" else "Create File List (V1)"
    }

    data object NameCircle : LXJob {
        override val title: String = "Name-Circle Relations"
    }

    data object MoveToTempScan : LXJob {
        override val title: String = "Move To Temp (Scan)"
    }

    data class MoveToTempMove(val items: Map<String, MoveToTemp.MoveInfo>) : LXJob {
        override val title: String = "Move To Temp"
    }

    data class MoveToTempRemove(val folders: Map<String, String>) : LXJob {
        override val title: String = "Remove Empty Folders"
    }

    data object MoveFromSourceScan : LXJob {
        override val title: String = "Move From Source (Scan)"
    }

    data class MoveFromSourceMove(val items: Map<String, MoveFromSource.MoveInfo>) : LXJob {
        override val title: String = "Move From Source"
    }
}

sealed interface JobState {
    data object Idle : JobState

    data class Running(
        val job: LXJob,
        val progressText: String = "Starting job...",
        val count: Int = 0
    ) : JobState

    data class Completed(
        val job: LXJob,
        val summary: String,
        val resultData: Any? = null
    ) : JobState

    data class Cancelled(
        val job: LXJob
    ) : JobState

    data class Failed(
        val job: LXJob,
        val errorMessage: String
    ) : JobState
}

object LXJobManager {

    private val _jobState = MutableStateFlow<JobState>(JobState.Idle)
    val jobState: StateFlow<JobState> = _jobState.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<String>>(emptyList())
    val recentLogs: StateFlow<List<String>> = _recentLogs.asStateFlow()

    private const val MAX_LOG_LINES = 200

    @Synchronized
    fun addLog(line: String) {
        val current = _recentLogs.value
        val updated = if (current.size >= MAX_LOG_LINES) {
            current.drop(current.size - MAX_LOG_LINES + 1) + line
        } else {
            current + line
        }
        _recentLogs.value = updated
    }

    @Synchronized
    fun clearLogs() {
        _recentLogs.value = emptyList()
    }

    fun isJobRunning(): Boolean = _jobState.value is JobState.Running

    @Volatile
    var isCancelled: Boolean = false
        private set

    @Synchronized
    fun startJob(context: Context, job: LXJob): Boolean {
        if (_jobState.value is JobState.Running) {
            return false
        }

        isCancelled = false
        _recentLogs.value = listOf("Starting ${job.title}...")
        _jobState.value = JobState.Running(job, "Starting ${job.title}...")

        // Stash the job payload into a static holder so the service can pick it up
        pendingJob = job

        val intent = Intent(context, LXJobService::class.java)
        ContextCompat.startForegroundService(context, intent)
        return true
    }

    @Synchronized
    fun cancelJob(context: Context) {
        if (_jobState.value is JobState.Running) {
            isCancelled = true
            addLog("Cancelling job...")
            val intent = Intent(context, LXJobService::class.java).apply {
                action = LXJobService.ACTION_CANCEL
            }
            context.startService(intent)
        }
    }

    @Synchronized
    fun notifyJobCancelled(job: LXJob) {
        val current = _jobState.value
        if (current is JobState.Running) {
            _jobState.value = JobState.Cancelled(job)
            addLog("⚠ ${job.title} aborted by user.")
        }
    }

    @Synchronized
    fun cancelJobForTesting() {
        val current = _jobState.value
        if (current is JobState.Running) {
            isCancelled = true
            _jobState.value = JobState.Cancelled(current.job)
            addLog("⚠ ${current.job.title} aborted by user.")
        }
    }

    @Synchronized
    fun startJobStateForTesting(job: LXJob) {
        isCancelled = false
        _recentLogs.value = listOf("Starting ${job.title}...")
        _jobState.value = JobState.Running(job, "Starting ${job.title}...")
    }

    internal var pendingJob: LXJob? = null

    @Synchronized
    fun updateProgress(progressText: String, count: Int = 0) {
        val current = _jobState.value
        if (current is JobState.Running) {
            _jobState.value = current.copy(progressText = progressText, count = count)
        }
    }

    @Synchronized
    fun completeJob(summary: String, resultData: Any? = null) {
        val current = _jobState.value
        if (current is JobState.Running) {
            _jobState.value = JobState.Completed(current.job, summary, resultData)
            addLog("✓ $summary")
        }
    }

    @Synchronized
    fun failJob(errorMessage: String) {
        val current = _jobState.value
        if (current is JobState.Running) {
            _jobState.value = JobState.Failed(current.job, errorMessage)
            addLog("ERROR: $errorMessage")
        }
    }

    @Synchronized
    fun resetToIdle() {
        _jobState.value = JobState.Idle
    }
}
