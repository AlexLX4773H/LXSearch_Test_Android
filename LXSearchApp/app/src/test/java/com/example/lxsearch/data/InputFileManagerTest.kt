package com.example.lxsearch.data

import org.junit.Assert.*
import org.junit.Test

class InputFileManagerTest {

    @Test
    fun testManagedFilesCount() {
        val files = InputFileManager.MANAGED_FILES
        assertEquals(4, files.size)

        val expectedNames = listOf(
            "exclude_folders_chapter_re.txt",
            "exclude_in_brackets.txt",
            "exclude_in_brackets_re.txt",
            "exclude_input_chapter_sep.txt"
        )
        assertEquals(expectedNames, files.map { it.fileName })
    }

    @Test
    fun testDefaultContentsNotEmpty() {
        for (file in InputFileManager.MANAGED_FILES) {
            assertTrue("Default content for ${file.fileName} should not be blank", file.defaultContent.isNotBlank())
            assertTrue("Syntax hint for ${file.fileName} should not be blank", file.syntaxHint.isNotBlank())
            assertTrue("Description for ${file.fileName} should not be blank", file.description.isNotBlank())
        }
    }

    @Test
    fun testComputeStats() {
        val sampleContent = "line1\nline2\n\nline3\n"
        val stats = InputFileManager.computeStats(sampleContent)

        assertEquals(5, stats.lineCount)
        assertEquals(3, stats.nonEmptyCount)
        assertTrue(stats.sizeBytes > 0)
    }

    @Test
    fun testGetFileInfo() {
        val info = InputFileManager.getFileInfo("exclude_input_chapter_sep.txt")
        assertNotNull(info)
        assertEquals("Chapter Sep", info?.shortName)

        val nonExistent = InputFileManager.getFileInfo("non_existent.txt")
        assertNull(nonExistent)
    }
}
