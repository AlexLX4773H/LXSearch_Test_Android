package com.example.lxsearch.service

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LXJobManagerTest {

    @Before
    fun setUp() {
        LXJobManager.resetToIdle()
        LXJobManager.clearLogs()
    }

    @Test
    fun testInitialState() {
        assertEquals(JobState.Idle, LXJobManager.jobState.value)
        assertTrue(LXJobManager.recentLogs.value.isEmpty())
        assertFalse(LXJobManager.isJobRunning())
    }

    @Test
    fun testLogBuffer() {
        for (i in 1..250) {
            LXJobManager.addLog("Line $i")
        }
        val logs = LXJobManager.recentLogs.value
        assertEquals(200, logs.size)
        assertEquals("Line 51", logs.first())
        assertEquals("Line 250", logs.last())
    }

    @Test
    fun testProgressUpdate() {
        // Manually simulate a running job transition without launching Android Service context
        val job = LXJob.CreateFileList(isV2 = true)
        val runningState = JobState.Running(job, "Starting...", 0)

        // Inject running state for test verification
        LXJobManager.startJobStateForTesting(job)
        assertTrue(LXJobManager.isJobRunning())

        LXJobManager.updateProgress("Processed 50 files", 50)
        val state = LXJobManager.jobState.value as JobState.Running
        assertEquals("Processed 50 files", state.progressText)
        assertEquals(50, state.count)
        assertEquals(job, state.job)
    }

    @Test
    fun testJobCompletion() {
        val job = LXJob.NameCircle
        LXJobManager.startJobStateForTesting(job)

        LXJobManager.completeJob("Finished 10 series", 10)
        val state = LXJobManager.jobState.value
        assertTrue(state is JobState.Completed)
        val completed = state as JobState.Completed
        assertEquals(job, completed.job)
        assertEquals("Finished 10 series", completed.summary)
        assertEquals(10, completed.resultData)

        assertTrue(LXJobManager.recentLogs.value.any { it.contains("✓ Finished 10 series") })
        assertFalse(LXJobManager.isJobRunning())
    }

    @Test
    fun testJobFailure() {
        val job = LXJob.MoveToTempScan
        LXJobManager.startJobStateForTesting(job)

        LXJobManager.failJob("Storage permission denied")
        val state = LXJobManager.jobState.value
        assertTrue(state is JobState.Failed)
        val failed = state as JobState.Failed
        assertEquals(job, failed.job)
        assertEquals("Storage permission denied", failed.errorMessage)

        assertTrue(LXJobManager.recentLogs.value.any { it.contains("ERROR: Storage permission denied") })
        assertFalse(LXJobManager.isJobRunning())
    }

    @Test
    fun testResetToIdle() {
        val job = LXJob.CreateFileList(isV2 = false)
        LXJobManager.startJobStateForTesting(job)
        assertTrue(LXJobManager.isJobRunning())

        LXJobManager.resetToIdle()
        assertEquals(JobState.Idle, LXJobManager.jobState.value)
        assertFalse(LXJobManager.isJobRunning())
    }

    @Test
    fun testCreateFileListJobAndRoute() {
        val v1Job = LXJob.CreateFileList(isV2 = false)
        assertEquals("Create File List (V1)", v1Job.title)
        assertFalse(v1Job.isV2)

        val v2Job = LXJob.CreateFileList(isV2 = true)
        assertEquals("Create File List (V2)", v2Job.title)
        assertTrue(v2Job.isV2)

        val defaultRoute = com.example.lxsearch.CreateFileListRoute()
        assertNull(defaultRoute.initialIsV2)

        val v2Route = com.example.lxsearch.CreateFileListRoute(initialIsV2 = true)
        assertEquals(true, v2Route.initialIsV2)
    }
}
