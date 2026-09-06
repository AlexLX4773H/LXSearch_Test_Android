package com.example.lxsearch.core

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Port of Running_standalone.py
 * Loads filename_list.csv into memory and provides search functionality
 * supporting multiple search modes (#, -, @, ~, default).
 */
class SearchEngine {

    data class SearchResult(val name: String, val folder: String)
    data class SearchResponse(
        val results: List<SearchResult>,
        val parsedTerms: List<String>,
        val count: Int,
        val found: Boolean
    )

    private var listDictData = mutableListOf<Map<String, Any>>()
    private var excludeInputChapterSep = mutableListOf<String>()
    private var isLoaded = false

    /**
     * Loads the CSV data and exclusion separators.
     * @param outputDir Directory containing filename_list.csv
     * @param inputDir Directory containing exclude_input_chapter_sep.txt
     */
    fun load(outputDir: File, inputDir: File): Boolean {
        listDictData.clear()
        excludeInputChapterSep.clear()

        // Load chapter separators
        val sepFile = File(inputDir, "exclude_input_chapter_sep.txt")
        if (sepFile.exists()) {
            sepFile.readLines(Charsets.UTF_8).forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) {
                    excludeInputChapterSep.add(" $trimmed ")
                }
            }
        }

        // Load data: prefer list.txt, fallback to filename_list.csv
        val listFile = File(outputDir, "list.txt")
        val csvFile = File(outputDir, "filename_list.csv")

        if (listFile.exists()) {
            try {
                BufferedReader(FileReader(listFile, Charsets.UTF_8)).use { reader ->
                    reader.forEachLine { line ->
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || !trimmed.contains(" ::: ")) return@forEachLine
                        val parts = trimmed.split(" ::: ", limit = 2)
                        val first = parts[0].trim()
                        val second = parts[1].trim()
                        val name = File(second).name
                        listDictData.add(
                            mapOf(
                                "Name" to name,
                                "Folder" to second,
                                "Name Pressed" to first
                            )
                        )
                    }
                }
                isLoaded = true
                return true
            } catch (_: Exception) {
                // fallback to csv
            }
        }

        if (!csvFile.exists()) return false

        try {
            BufferedReader(FileReader(csvFile, Charsets.UTF_8)).use { reader ->
                val headersLine = reader.readLine() ?: return false
                val headers = headersLine.split(LIST_CSV_DELIMITER)

                reader.forEachLine { line ->
                    if (line.isBlank()) return@forEachLine
                    val values = line.split(LIST_CSV_DELIMITER)
                    val entry = mutableMapOf<String, Any>()

                    headers.forEachIndexed { index, header ->
                        if (index < values.size) {
                            if (header in LIST_CSV_HEADERS_THAT_ARE_LIST) {
                                // Parse list string like "['item1', 'item2']"
                                entry[header] = parseListString(values[index])
                            } else {
                                entry[header] = values[index]
                            }
                        }
                    }
                    listDictData.add(entry)
                }
            }
            isLoaded = true
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun isDataLoaded(): Boolean = isLoaded
    fun getDataCount(): Int = listDictData.size

    /**
     * Parses a Python list string representation into a Kotlin list.
     * e.g., "['item1', 'item2']" -> ["item1", "item2"]
     */
    private fun parseListString(input: String): List<String> {
        val trimmed = input.trim()
        if (trimmed == "[]" || trimmed.isEmpty()) return emptyList()
        // Remove outer brackets and split
        val inner = trimmed.removePrefix("[").removeSuffix("]")
        return inner.split(",").map { it.trim().removeSurrounding("'").removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
    }

    /**
     * Searches the loaded data.
     * @param query The search query, optionally prefixed with #, -, @, ~
     * @return SearchResponse with results, parsed terms, and count
     */
    fun search(query: String): SearchResponse {
        if (!isLoaded || query.isBlank()) {
            return SearchResponse(emptyList(), emptyList(), 0, false)
        }

        return when {
            query.startsWith('#') -> searchExact(query.substring(1))
            query.startsWith('-') -> searchDashSep(query.substring(1))
            query.startsWith('@') && query.length > 1 -> searchCustomSep(query[1].toString(), query.substring(2))
            query.startsWith('~') -> searchStripBlocks(query.substring(1))
            else -> searchDefault(query)
        }
    }

    /** #text : Exact search in Folder column */
    private fun searchExact(text: String): SearchResponse {
        val vals = listOf(text)
        val results = checkList(vals, "Folder")
        return SearchResponse(results, vals, results.size, results.isNotEmpty())
    }

    /** -text : Use '-' as additional separator */
    private fun searchDashSep(text: String): SearchResponse {
        val parsed = parseInputValueCustom(text, INPUT_SEPARATORS + "-")
        @Suppress("UNCHECKED_CAST")
        var vals = (parsed["Main Titles"] as? List<String>) ?: emptyList()
        vals = splitStringAndReturn1(vals, excludeInputChapterSep)
        vals = stringPressList(vals)
        vals = vals.map { it.replace(Regex("""\d+$"""), "") }
        val results = checkList(vals, "Name Pressed")
        return SearchResponse(results, vals, results.size, results.isNotEmpty())
    }

    /** @<c>text : Use <c> as custom separator */
    private fun searchCustomSep(separator: String, text: String): SearchResponse {
        val parsed = parseInputValueCustom(text, INPUT_SEPARATORS + separator)
        @Suppress("UNCHECKED_CAST")
        var vals = (parsed["Main Titles"] as? List<String>) ?: emptyList()
        vals = splitStringAndReturn1(vals, excludeInputChapterSep)
        vals = stringPressList(vals)
        vals = vals.map { it.replace(Regex("""\d+$"""), "") }
        val results = checkList(vals, "Name Pressed")
        return SearchResponse(results, vals, results.size, results.isNotEmpty())
    }

    /** ~text : Strip ~...~ blocks from input */
    private fun searchStripBlocks(text: String): SearchResponse {
        if (text.count { it == '~' } % 2 != 0) {
            return SearchResponse(emptyList(), emptyList(), 0, false)
        }
        val remainingVal = text.replace(Regex("~[^~]*~"), "")
        val parsed = parseInputValue(remainingVal)
        @Suppress("UNCHECKED_CAST")
        var vals = (parsed["Main Titles"] as? List<String>) ?: emptyList()
        vals = splitStringAndReturn1(vals, excludeInputChapterSep)
        vals = stringPressList(vals)
        vals = vals.map { it.replace(Regex("""\d+$"""), "") }
        val results = checkList(vals, "Name Pressed")
        return SearchResponse(results, vals, results.size, results.isNotEmpty())
    }

    /** Default search: parse input value and search Name Pressed */
    private fun searchDefault(text: String): SearchResponse {
        val parsed = parseInputValue(text)
        @Suppress("UNCHECKED_CAST")
        var vals = (parsed["Main Titles"] as? List<String>) ?: emptyList()
        vals = splitStringAndReturn1(vals, excludeInputChapterSep)
        vals = stringPressList(vals)
        vals = vals.map { it.replace(Regex("""\d+$"""), "") }
        val results = checkList(vals, "Name Pressed")
        return SearchResponse(results, vals, results.size, results.isNotEmpty())
    }

    /**
     * Checks all loaded data against the search values.
     * Port of check_list() from the Python script.
     */
    private fun checkList(stringInputValues: List<String>, columnType: String = "Name Pressed"): List<SearchResult> {
        val resultSet = mutableSetOf<SearchResult>()

        for (value in stringInputValues) {
            if (value.length < 3) continue
            for (lineDict in listDictData) {
                val columnValue = (lineDict[columnType] as? String) ?: ""
                if (value.lowercase() in columnValue.lowercase()) {
                    val name = (lineDict["Name"] as? String) ?: ""
                    val folder = (lineDict["Folder"] as? String) ?: ""
                    resultSet.add(SearchResult(name, folder))
                }
            }
        }

        return resultSet.toList()
    }
}
