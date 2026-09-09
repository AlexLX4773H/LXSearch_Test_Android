package com.example.lxsearch.core

import org.junit.Assert.*
import org.junit.Test

class SmbTest {

    @Test
    fun testSocketReachability() {
        val reachable = SmbScanner.isSmbReachable("192.168.88.234", 445, 2000)
        println("Socket reachable 192.168.88.234: $reachable")
        assertTrue(reachable)

        val unreachable = SmbScanner.isSmbReachable("192.168.88.239", 445, 1000)
        println("Socket reachable 192.168.88.239: $unreachable")
        assertFalse(unreachable)
    }

    @Test
    fun testParseSmbUri() {
        val comp = SmbScanner.parseSmbUri("""\\192.168.88.234\Share2sgb\Manga CBZ\Doujinshi\Archived""")
        assertNotNull(comp)
        assertEquals("192.168.88.234", comp?.host)
        assertEquals("Share2sgb", comp?.share)
        assertEquals("Manga CBZ\\Doujinshi\\Archived", comp?.subPath)
    }

    @Test
    fun testSmbScanFolders() {
        val sharePath = """\\192.168.88.234\Share2sgb"""
        val locations = listOf("""\\192.168.88.234\Share2sgb\Manga CBZ\Doujinshi\Archived""")
        val items = SmbScanner.scanFolders(locations, sharePath, "alex", "aaaaaaaa")
        println("Scanned basic items: ${items.size}")
        assertTrue(items.isNotEmpty())
        assertTrue("Should contain directories", items.any { it.isDirectory })
        assertTrue("Should contain archive files", items.any { !it.isDirectory && it.name.endsWith(".cbz", ignoreCase = true) })
        assertTrue("Should not contain images", items.none { !it.isDirectory && IMAGE_EXTENSIONS.contains("." + it.name.substringAfterLast('.').lowercase()) })
    }

    @Test
    fun testSmbScanFoldersWithDetails() {
        val sharePath = """\\192.168.88.234\Share2sgb"""
        val locations = listOf("""\\192.168.88.234\Share2sgb\Manga CBZ\Doujinshi\Archived""")
        val items = SmbScanner.scanFoldersWithDetails(locations, sharePath, "alex", "aaaaaaaa")
        println("Scanned details items: ${items.size}")
        assertTrue(items.isNotEmpty())
        assertTrue("Should contain directories", items.any { it.isDirectory })
        val fileItem = items.firstOrNull { !it.isDirectory && it.name.endsWith(".cbz", ignoreCase = true) }
        assertNotNull("Should contain at least one archive file", fileItem)
        assertEquals(1, fileItem?.itemCount)
        assertFalse(fileItem?.hasFolders ?: true)
        assertTrue((fileItem?.totalSizeBytes ?: 0L) > 0L)
        assertTrue("Should not contain images", items.none { !it.isDirectory && IMAGE_EXTENSIONS.contains("." + it.name.substringAfterLast('.').lowercase()) })
    }

    @Test
    fun testUnreachableServerDoesNotThrow() {
        val sharePath = """\\192.168.88.239\Share2sgb"""
        val locations = listOf("""\\192.168.88.239\Share2sgb\Manga CBZ\Doujinshi\Archived""")
        val folders = SmbScanner.scanFolders(locations, sharePath, "alex", "aaaaaaaa")
        assertTrue(folders.isEmpty())
    }
}
