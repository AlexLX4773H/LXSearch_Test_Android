package com.example.lxsearch.core

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SearchEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSearchEngineLoadsFromListTxt() {
        val root = tempFolder.newFolder("search_test")
        val outputDir = File(root, "output").apply { mkdirs() }
        val inputDir = File(root, "input").apply { mkdirs() }

        val listFile = File(outputDir, "list.txt")
        listFile.writeText(
            """
            circletestmangaauthor ::: /storage/emulated/0/Books/[Circle] Test Manga [Author]
            standalonebookauthorcbz ::: /storage/emulated/0/Books/[Circle] Standalone Book [Author].cbz
            """.trimIndent()
        )

        val engine = SearchEngine()
        val loaded = engine.load(outputDir, inputDir)

        assertTrue("SearchEngine should load successfully from list.txt", loaded)
        assertEquals(2, engine.getDataCount())

        // Test default search
        val res1 = engine.search("Test Manga")
        assertTrue(res1.found)
        assertEquals(1, res1.count)
        assertEquals("[Circle] Test Manga [Author]", res1.results[0].name)
        assertEquals("/storage/emulated/0/Books/[Circle] Test Manga [Author]", res1.results[0].folder)

        // Test search on standalone file
        val res2 = engine.search("Standalone Book")
        assertTrue(res2.found)
        assertEquals(1, res2.count)
        assertEquals("[Circle] Standalone Book [Author].cbz", res2.results[0].name)

        // Test exact folder search with #
        val resExact = engine.search("#Test Manga")
        assertTrue(resExact.found)
        assertEquals(1, resExact.count)
        assertEquals("[Circle] Test Manga [Author]", resExact.results[0].name)

        // Test default search with 'MTL' at the end
        val resMtl = engine.search("Test Manga MTL")
        assertTrue("Searching with 'Test Manga MTL' should find the book", resMtl.found)
        assertEquals(1, resMtl.count)
        assertEquals("[Circle] Test Manga [Author]", resMtl.results[0].name)

        // Test default search with ' - MTL' at the end
        val resDashMtl = engine.search("Test Manga - MTL")
        assertTrue("Searching with 'Test Manga - MTL' should find the book", resDashMtl.found)
        assertEquals(1, resDashMtl.count)
        assertEquals("[Circle] Test Manga [Author]", resDashMtl.results[0].name)

        // Test default search with lowercase 'mtl' and trailing spaces
        val resLowerMtl = engine.search("Test Manga mtl ")
        assertTrue("Searching with 'Test Manga mtl ' should find the book", resLowerMtl.found)
        assertEquals(1, resLowerMtl.count)

        // Test default search with 'MTL [Author]'
        val resMtlBracket = engine.search("Test Manga MTL [Author]")
        assertTrue("Searching with 'Test Manga MTL [Author]' should find the book", resMtlBracket.found)
        assertEquals(1, resMtlBracket.count)
    }

    @Test
    fun testSearchEngineFallbackToCsv() {
        val root = tempFolder.newFolder("fallback_test")
        val outputDir = File(root, "output").apply { mkdirs() }
        val inputDir = File(root, "input").apply { mkdirs() }

        val csvFile = File(outputDir, "filename_list.csv")
        csvFile.writeText(
            LIST_CSV_HEADERS.joinToString("╥") + "\n" +
            listOf(
                "[Circle] Fallback Manga [Author]",
                "/storage/emulated/0/Books/[Circle] Fallback Manga [Author]",
                "Fallback Manga",
                "['Circle']", "[]", "[]", "['Author']", "[]",
                "['Fallback Manga']", "['fallbackmanga']",
                "circlefallbackmangaauthor"
            ).joinToString("╥") + "\n"
        )

        val engine = SearchEngine()
        val loaded = engine.load(outputDir, inputDir)

        assertTrue("SearchEngine should fall back to filename_list.csv", loaded)
        assertEquals(1, engine.getDataCount())

        val res = engine.search("Fallback Manga")
        assertTrue(res.found)
        assertEquals(1, res.count)
    }
}
