package com.example.lxsearch.data

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JobHistoryManagerTest {

    @Test
    fun testJobKeys() {
        val keys = JobHistoryManager.JobKey.values()
        assertEquals(3, keys.size)

        val v1 = JobHistoryManager.JobKey.CREATE_FILE_LIST_V1
        assertEquals("last_run_v1", v1.prefKey)
        assertEquals("filename_list.csv", v1.fileName)

        val v2 = JobHistoryManager.JobKey.CREATE_FILE_LIST_V2
        assertEquals("last_run_v2", v2.prefKey)
        assertEquals("filename_list_v2.csv", v2.fileName)

        val nc = JobHistoryManager.JobKey.NAME_CIRCLE
        assertEquals("last_run_name_circle", nc.prefKey)
        assertEquals("name_circle_relation.json", nc.fileName)
    }

    @Test
    fun testFormatTimestamp() {
        val testTime = 1700000000000L // 2023-11-14 ...
        val expected = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(testTime))
        val formatted = JobHistoryManager.formatTimestamp(testTime)
        assertEquals(expected, formatted)
        assertTrue(formatted.matches(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}""")))
    }
}
