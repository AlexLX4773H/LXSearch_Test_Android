package com.example.lxsearch.core

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class CreateFileListTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testCreateFileListV1IncludesFilesScanInCsv() {
        val root = tempFolder.newFolder("v1_test")
        val outputDir = File(root, "output")
        val inputDir = File(root, "input").apply { mkdirs() }
        File(inputDir, "exclude_folders_chapter_re.txt").writeText(
            """
            [\s\d-]*chapter[\s\d]*$
            oneshot
            [.]*nomedia
            """.trimIndent()
        )

        val foldersDir = File(root, "folders").apply { mkdirs() }
        val mangaFolder = File(foldersDir, "[Circle] Test Folder [Author]").apply { mkdirs() }
        File(mangaFolder, "page01.jpg").writeText("dummy image")
        // Excluded folder
        val chapterFolder = File(foldersDir, "Chapter 01").apply { mkdirs() }
        File(chapterFolder, "page01.jpg").writeText("dummy image")

        val filesDir = File(root, "files").apply { mkdirs() }
        val standaloneFile = File(filesDir, "[Circle] Standalone Book [Author].cbz")
        standaloneFile.writeText("sample cbz content")

        // Excluded files
        File(filesDir, ".nomedia").writeText("")
        File(filesDir, "Chapter 02.cbz").writeText("chapter content")
        File(filesDir, "oneshot.zip").writeText("oneshot content")

        val count = CreateFileListV1.run(
            outputDir = outputDir,
            inputDir = inputDir,
            foldersDirs = listOf(foldersDir.absolutePath),
            filesDirs = listOf(filesDir.absolutePath)
        )

        // Only Test Folder and Standalone Book should be included
        assertEquals(2, count)

        val csvFile = File(outputDir, "filename_list.csv")
        assertTrue(csvFile.exists())

        val lines = csvFile.readLines(Charsets.UTF_8)
        assertEquals(3, lines.size) // Header + 1 folder + 1 file

        val header = lines[0].split(LIST_CSV_DELIMITER)
        assertEquals(11, header.size)

        val folderRow = lines[1].split(LIST_CSV_DELIMITER)
        assertEquals(11, folderRow.size)
        assertEquals("[Circle] Test Folder [Author]", folderRow[0])
        assertEquals(mangaFolder.absolutePath, folderRow[1])

        val fileRow = lines[2].split(LIST_CSV_DELIMITER)
        assertEquals(11, fileRow.size)
        assertEquals("[Circle] Standalone Book [Author].cbz", fileRow[0])
        assertEquals(standaloneFile.absolutePath, fileRow[1])

        // Verify .nomedia and chapter files are not in CSV
        assertFalse(lines.any { it.contains(".nomedia") })
        assertFalse(lines.any { it.contains("Chapter 01") })
        assertFalse(lines.any { it.contains("Chapter 02") })
        assertFalse(lines.any { it.contains("oneshot") })
    }

    @Test
    fun testCreateFileListV2IncludesFilesScanInCsv() {
        val root = tempFolder.newFolder("v2_test")
        val outputDir = File(root, "output")
        val inputDir = File(root, "input").apply { mkdirs() }
        File(inputDir, "exclude_folders_chapter_re.txt").writeText(
            """
            [\s\d-]*chapter[\s\d]*$
            oneshot
            [.]*nomedia
            """.trimIndent()
        )

        val foldersDir = File(root, "folders").apply { mkdirs() }
        val mangaFolder = File(foldersDir, "[Circle] Test Folder [Author]").apply { mkdirs() }
        File(mangaFolder, "001.jpg").writeText("dummy image")
        // Excluded folder
        val chapterFolder = File(foldersDir, "Chapter 01").apply { mkdirs() }
        File(chapterFolder, "001.jpg").writeText("dummy image")

        val filesDir = File(root, "files").apply { mkdirs() }
        val standaloneFile = File(filesDir, "[Circle] Standalone Archive [Author].zip")
        standaloneFile.writeText("sample zip content of certain bytes")

        // Excluded files
        File(filesDir, ".nomedia").writeText("")
        File(filesDir, "Chapter 03.cbz").writeText("chapter content")
        File(filesDir, "oneshot.zip").writeText("oneshot content")

        val count = CreateFileListV2.run(
            outputDir = outputDir,
            inputDir = inputDir,
            foldersDirs = listOf(foldersDir.absolutePath),
            filesDirs = listOf(filesDir.absolutePath)
        )

        assertEquals(2, count)

        val csvFile = File(outputDir, "filename_list_v2.csv")
        assertTrue(csvFile.exists())

        val lines = csvFile.readLines(Charsets.UTF_8)
        assertEquals(3, lines.size) // Header + 1 folder + 1 file

        val header = lines[0].split(LIST_CSV_DELIMITER)
        assertEquals(25, header.size)

        val folderRow = lines[1].split(LIST_CSV_DELIMITER)
        assertEquals(25, folderRow.size)
        assertEquals("[Circle] Test Folder [Author]", folderRow[0])
        assertEquals(mangaFolder.absolutePath, folderRow[1])

        val fileRow = lines[2].split(LIST_CSV_DELIMITER)
        assertEquals(25, fileRow.size)
        assertEquals("[Circle] Standalone Archive [Author].zip", fileRow[0])
        assertEquals(standaloneFile.absolutePath, fileRow[1])

        // Verify end items for file: Count Items = 1, Has Folders = false, Count Files = 1
        assertEquals("1", fileRow[20]) // Count Items
        assertEquals("false", fileRow[21]) // Has Folders
        assertEquals("1", fileRow[23]) // Count Files
        assertEquals(fileRow[22], fileRow[24]) // Total Size == Average Size

        // Verify excluded items
        assertFalse(lines.any { it.contains(".nomedia") })
        assertFalse(lines.any { it.contains("Chapter 01") })
        assertFalse(lines.any { it.contains("Chapter 03") })
        assertFalse(lines.any { it.contains("oneshot") })
    }
}
