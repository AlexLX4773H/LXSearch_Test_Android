package com.example.lxsearch.data

import android.content.Context
import com.example.lxsearch.MainActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object JobHistoryManager {

    private const val PREFS_NAME = "lx_job_history"

    enum class JobKey(val prefKey: String, val fileName: String) {
        CREATE_FILE_LIST_V1("last_run_v1", "filename_list.csv"),
        CREATE_FILE_LIST_V2("last_run_v2", "filename_list_v2.csv"),
        NAME_CIRCLE("last_run_name_circle", "name_circle_relation.json")
    }

    fun recordLastRun(context: Context, jobKey: JobKey, timestamp: Long = System.currentTimeMillis()) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(jobKey.prefKey, timestamp).apply()
    }

    fun getLastRunTimestamp(context: Context, jobKey: JobKey): Long? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getLong(jobKey.prefKey, 0L)
        if (saved > 0L) return saved

        // Fallback to output file last modified time if exists
        try {
            val outputDir = MainActivity.getOutputDir(context)
            val file = File(outputDir, jobKey.fileName)
            if (file.exists()) {
                val modTime = file.lastModified()
                if (modTime > 0L) return modTime
            }
        } catch (_: Throwable) {
            // Ignore error during directory resolution
        }

        return null
    }

    fun getLastRunFormatted(context: Context, jobKey: JobKey): String? {
        val timestamp = getLastRunTimestamp(context, jobKey) ?: return null
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
