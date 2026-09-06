package com.example.lxsearch.core

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class UtilityFunctionsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testIsMihonDownloadsDir() {
        assertTrue(isMihonDownloadsDir("/storage/emulated/0/Vere2/Vere/Mihon/downloads"))
        assertTrue(isMihonDownloadsDir("/storage/emulated/0/Vere2/Vere/Mihon/downloads/"))
        assertTrue(isMihonDownloadsDir("storage/emulated/0/Vere2/Vere/Mihon/downloads"))
        assertTrue(isMihonDownloadsDir("storage/emulated/0/Vere2/Vere/Mihon/downloads/"))
        assertTrue(isMihonDownloadsDir("  /storage/emulated/0/Vere2/Vere/Mihon/downloads\\  "))

        assertFalse(isMihonDownloadsDir("/storage/emulated/0/Vere2/TempD"))
        assertFalse(isMihonDownloadsDir("/storage/emulated/0/Vere2/Vere/NewFolder"))
        assertFalse(isMihonDownloadsDir("/storage/emulated/0/Vere2/Vere/Mihon/other"))
    }

    @Test
    fun testGetMihonHentaiFoldersFiltersMatchingSubdirectories() {
        val mockDownloads = tempFolder.newFolder("downloads")

        // Folders containing "Hentai" (varying casing)
        val hentaiDir1 = File(mockDownloads, "NineHentai (EN)").apply { mkdir() }
        val hentaiDir2 = File(mockDownloads, "nHentai.com (unoriginal) (EN)").apply { mkdir() }
        val hentaiDir3 = File(mockDownloads, "E-Hentai (EN)").apply { mkdir() }
        val hentaiDir4 = File(mockDownloads, "HentaiHand (ALL)").apply { mkdir() }

        // Folders NOT containing "Hentai"
        File(mockDownloads, "Hennojin (EN)").mkdir()
        File(mockDownloads, "MangaDex (EN)").mkdir()
        File(mockDownloads, "Webtoons").mkdir()

        // Non-directory file containing "Hentai"
        File(mockDownloads, "some_hentai_file.txt").writeText("dummy content")

        val result = getMihonHentaiFolders(mockDownloads.absolutePath)

        assertEquals(4, result.size)
        assertTrue(result.contains(hentaiDir1.absolutePath))
        assertTrue(result.contains(hentaiDir2.absolutePath))
        assertTrue(result.contains(hentaiDir3.absolutePath))
        assertTrue(result.contains(hentaiDir4.absolutePath))

        // Ensure non-hentai folders and files are excluded
        assertFalse(result.any { it.contains("Hennojin") })
        assertFalse(result.any { it.contains("MangaDex") })
        assertFalse(result.any { it.contains("Webtoons") })
        assertFalse(result.any { it.contains("some_hentai_file.txt") })
    }

    @Test
    fun testDefaultMihonHentaiFoldersAllContainHentai() {
        // Fallback default list must contain only folders with 'Hentai'
        for (folder in DEFAULT_MIHON_HENTAI_FOLDERS) {
            val folderName = File(folder).name
            assertTrue(
                "Folder $folderName should contain 'Hentai'",
                folderName.contains("Hentai", ignoreCase = true)
            )
        }
        // Ensure Hennojin is NOT in the default fallback list
        assertFalse(DEFAULT_MIHON_HENTAI_FOLDERS.any { it.contains("Hennojin") })
    }

    @Test
    fun testResolveReadDirectoriesExpandsMihonDownloads() {
        val dirs = listOf(
            "/storage/emulated/0/Vere2/TempD",
            "/storage/emulated/0/Vere2/Vere/Mihon/downloads/",
            "/storage/emulated/0/Vere2/TempT"
        )

        val resolved = resolveReadDirectories(dirs)

        // Should retain TempD and TempT
        assertTrue(resolved.contains("/storage/emulated/0/Vere2/TempD"))
        assertTrue(resolved.contains("/storage/emulated/0/Vere2/TempT"))

        // Should NOT contain the base downloads directory itself
        assertFalse(resolved.contains("/storage/emulated/0/Vere2/Vere/Mihon/downloads/"))
        assertFalse(resolved.contains("/storage/emulated/0/Vere2/Vere/Mihon/downloads"))

        // Should contain Hentai subdirectories
        assertTrue(resolved.any { it.contains("NineHentai") })
        assertTrue(resolved.any { it.contains("NHentai") })
    }

    @Test
    fun testReadRootDirAndCandidateSourcesContainOnlyHentaiForMihon() {
        // Test dynamic properties
        val readDirs = READ_ROOT_DIR_FOR_FOLDERS
        val candidateDirs = CANDIDATE_SOURCES

        val mihonReadDirs = readDirs.filter { it.contains("/Mihon/downloads") }
        assertTrue(mihonReadDirs.isNotEmpty())
        for (dir in mihonReadDirs) {
            val name = File(dir).name
            assertTrue("Mihon folder $name should contain Hentai", name.contains("Hentai", ignoreCase = true))
        }

        val mihonCandidates = candidateDirs.filter { it.contains("/Mihon/downloads") }
        assertTrue(mihonCandidates.isNotEmpty())
        for (dir in mihonCandidates) {
            val name = File(dir).name
            assertTrue("Mihon candidate $name should contain Hentai", name.contains("Hentai", ignoreCase = true))
        }
    }

    @Test
    fun testSafeAtomicReplace() {
        val destFile = tempFolder.newFile("target.csv").apply { writeText("original content") }
        val tempFile = tempFolder.newFile("target.csv.tmp").apply { writeText("new completed content") }

        safeAtomicReplace(tempFile, destFile)

        assertEquals("new completed content", destFile.readText())
        assertFalse(tempFile.exists())
    }
}
