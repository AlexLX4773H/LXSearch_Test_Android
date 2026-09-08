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
        val folders = SmbScanner.scanFolders(locations, sharePath, "alex", "aaaaaaaa")
        println("Scanned basic folders: $folders")
        assertEquals(1, folders.size)
        assertEquals("Test11", folders[0].name)
        assertEquals("""\\192.168.88.234\Share2sgb\Manga CBZ\Doujinshi\Archived\Test11""", folders[0].uncPath)
    }

    @Test
    fun testSmbScanFoldersWithDetails() {
        val sharePath = """\\192.168.88.234\Share2sgb"""
        val locations = listOf("""\\192.168.88.234\Share2sgb\Manga CBZ\Doujinshi\Archived""")
        val folders = SmbScanner.scanFoldersWithDetails(locations, sharePath, "alex", "aaaaaaaa")
        println("Scanned details folders: $folders")
        assertEquals(1, folders.size)
        assertEquals("Test11", folders[0].name)
        assertEquals(0, folders[0].itemCount)
        assertFalse(folders[0].hasFolders)
    }

    @Test
    fun testUnreachableServerDoesNotThrow() {
        val sharePath = """\\192.168.88.239\Share2sgb"""
        val locations = listOf("""\\192.168.88.239\Share2sgb\Manga CBZ\Doujinshi\Archived""")
        val folders = SmbScanner.scanFolders(locations, sharePath, "alex", "aaaaaaaa")
        assertTrue(folders.isEmpty())
    }
}
