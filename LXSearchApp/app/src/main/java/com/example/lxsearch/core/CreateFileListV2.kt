package com.example.lxsearch.core

import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.StringReader

/**
 * Port of Run_Create_file_list_v2_standalone.py
 * Enhanced version that also reads ComicInfo.xml/json and extracts
 * parodies, groups, characters, genre, writer, penciller, file stats.
 */
object CreateFileListV2 {

    data class Progress(val current: Int, val name: String, val done: Boolean = false)

    // ── ComicInfo XML Parsing ──

    private fun cleanXmlTag(tag: String): String {
        return if ('}' in tag) tag.substringAfter('}') else tag
    }

    /** Gets text content of a tag from XML string. */
    private fun getTagTextXml(tagNames: List<String>, xmlContent: String): String {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val parser = factory.newPullParser()
            parser.setInput(StringReader(xmlContent))
            val lowerNames = tagNames.map { it.lowercase() }

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = cleanXmlTag(parser.name).lowercase()
                    if (tagName in lowerNames) {
                        val text = parser.nextText()?.trim() ?: ""
                        if (text.isNotEmpty()) return text
                    }
                }
                eventType = parser.next()
            }
        } catch (_: Exception) {}
        return ""
    }

    /** Extracts summary items (Parodies, Groups, etc.) from summary text. */
    private fun getSummaryItems(summaryText: String, xmlContent: String? = null): List<String> {
        val patterns = mapOf(
            "Parodies" to Regex("""^\s*(?:\*\*)?\s*parod(?:ies|y)\s*(?:\*\*)?\s*:\s*(.+)$""", RegexOption.IGNORE_CASE),
            "Groups" to Regex("""^\s*(?:\*\*)?\s*(?:groups?|circles?)\s*(?:\*\*)?\s*:\s*(.+)$""", RegexOption.IGNORE_CASE),
            "Characters" to Regex("""^\s*(?:\*\*)?\s*characters?\s*(?:\*\*)?\s*:\s*(.+)$""", RegexOption.IGNORE_CASE),
            "Pages" to Regex("""^\s*(?:\*\*)?\s*pages?\s*(?:\*\*)?\s*:\s*(.+)$""", RegexOption.IGNORE_CASE),
            "Language" to Regex("""^\s*(?:\*\*)?\s*languages?\s*(?:\*\*)?\s*:\s*(.+)$""", RegexOption.IGNORE_CASE),
            "Categories" to Regex("""^\s*(?:\*\*)?\s*categor(?:ies|y)\s*(?:\*\*)?\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)
        )
        val seriesPat = Regex("""^\s*(?:\*\*)?\s*series\s*(?:\*\*)?\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)

        val result = mutableMapOf<String, String>()
        V2_LIST_SUMMARY.forEach { result[it] = "" }
        var seriesVal = ""

        if (summaryText.isNotEmpty()) {
            for (line in summaryText.lines()) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                var matched = false
                for ((k, pat) in patterns) {
                    if (result[k].isNullOrEmpty()) {
                        val m = pat.find(trimmed)
                        if (m != null) {
                            result[k] = m.groupValues[1].trim()
                            matched = true
                            break
                        }
                    }
                }
                if (!matched && seriesVal.isEmpty()) {
                    val mSer = seriesPat.find(trimmed)
                    if (mSer != null) {
                        seriesVal = mSer.groupValues[1].trim()
                    }
                }
            }
        }

        // Fallback for Parodies from Series
        if (result["Parodies"].isNullOrEmpty() && seriesVal.isNotEmpty()) {
            result["Parodies"] = seriesVal
        }

        // Fallback to direct XML tags
        if (xmlContent != null) {
            val tagFallbacks = listOf(
                "Parodies" to listOf("Parodies", "Parody"),
                "Groups" to listOf("Groups", "Group", "Teams", "Team", "Circle", "Circles"),
                "Characters" to listOf("Characters", "Character"),
                "Pages" to listOf("Pages", "Page", "PageCount"),
                "Language" to listOf("Language", "Languages", "LanguageISO"),
                "Categories" to listOf("Categories", "Category", "Format")
            )
            for ((key, tags) in tagFallbacks) {
                if (result[key].isNullOrEmpty()) {
                    val value = getTagTextXml(tags, xmlContent)
                    if (value.isNotEmpty()) result[key] = value
                }
            }
        }

        return V2_LIST_SUMMARY.map { result[it] ?: "" }
    }

    /** Gets all V2 items from XML content (6 summary + 3 list = 9 items). */
    private fun getItemsXml(xmlContent: String): List<String> {
        val summaryText = getTagTextXml(listOf("Summary", "Description", "Notes"), xmlContent)
        val itemList = getSummaryItems(summaryText, xmlContent).toMutableList()

        val genre = getTagTextXml(listOf("Genre", "Genres", "Tags"), xmlContent)
        val writer = getTagTextXml(listOf("Writer", "Writers", "Author", "Authors"), xmlContent)
        val penciller = getTagTextXml(listOf("Penciller", "Pencillers", "Artist", "Artists", "Illustrator"), xmlContent)

        itemList.addAll(listOf(genre, writer, penciller))
        return itemList
    }

    /** Gets all V2 items from JSON content. */
    private fun getItemsJson(json: JSONObject): List<String> {
        fun getStr(vararg keys: String): String {
            for (k in keys) {
                val v = json.optString(k, "")
                if (v.isNotEmpty()) return v
            }
            return ""
        }
        return listOf(
            getStr("parodies", "parody"),
            getStr("groups", "group", "circle"),
            getStr("characters", "character"),
            getStr("pages", "page_count", "pageCount"),
            getStr("language", "languages"),
            getStr("categories", "category"),
            getStr("genre", "tags"),
            getStr("writer", "author"),
            getStr("penciller", "artist")
        )
    }

    /** Parses ComicInfo from a file (XML or JSON). */
    private fun parseComicInfo(file: File): List<String> {
        if (!file.exists()) return List(9) { "" }
        try {
            val content = file.readText(Charsets.UTF_8)
            return if (file.name.lowercase().endsWith(".json")) {
                getItemsJson(JSONObject(content))
            } else {
                getItemsXml(content)
            }
        } catch (_: Exception) {}
        return List(9) { "" }
    }

    /** Finds ComicInfo.xml/json in folder or immediate subdirectories. */
    private fun getComicInfoFile(folderPath: File): File? {
        try {
            if (!folderPath.exists() || !folderPath.isDirectory) return null

            // Check direct folder
            for (file in folderPath.listFiles() ?: emptyArray()) {
                if (file.name.lowercase() in VALID_XML_NAMES) return file
            }

            // Check immediate subdirectories
            for (item in folderPath.listFiles() ?: emptyArray()) {
                if (item.isDirectory) {
                    for (subFile in item.listFiles() ?: emptyArray()) {
                        if (subFile.name.lowercase() in VALID_XML_NAMES) return subFile
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /** Checks if directory has any subdirectories. */
    private fun checkDirectoryExists(path: File): Boolean {
        return path.listFiles()?.any { it.isDirectory } ?: false
    }

    /** Gets total size, file count, and average size of valid image files. */
    private fun getFilesSizeAndCountAvg(path: File): Triple<String, Int, String> {
        var totalSize = 0L
        var count = 0

        for (item in path.listFiles() ?: emptyArray()) {
            if (item.isFile && item.extension.lowercase().let { ".$it" in V2_VALID_EXT }) {
                totalSize += item.length()
                count++
            }
        }

        val avg = if (count > 0) totalSize / count else 0L
        return Triple(convertBytesToReadableSize(totalSize), count, convertBytesToReadableSize(avg))
    }

    /**
     * Runs the file list creation process (v2).
     * @param outputDir The directory to write output files to
     * @param inputDir The directory containing exclusion text files
     * @param onProgress Callback for progress updates
     * @return Total count of processed items
     */
    fun run(
        outputDir: File,
        inputDir: File,
        foldersDirs: List<String> = READ_ROOT_DIR_FOR_FOLDERS,
        filesDirs: List<String> = READ_ROOT_DIR_FOR_FILES,
        isCancelled: () -> Boolean = { false },
        onProgress: (Progress) -> Unit = {}
    ): Int {
        outputDir.mkdirs()

        // Load chapter exclusion patterns
        val excludeChapterRe = mutableListOf<String>()
        val excludeFile = File(inputDir, "exclude_folders_chapter_re.txt")
        if (excludeFile.exists()) {
            excludeFile.readLines(Charsets.UTF_8).forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty()) excludeChapterRe.add(trimmed)
            }
        }

        var count = 0
        val tempStringBuilder = StringBuilder()
        val finalList = mutableListOf<List<Any>>()
        finalList.add(V2_LIST_CSV_HEADERS.map { it })

        // Scan directories for folders
        val dirsToScan = resolveReadDirectories(foldersDirs)
        for (rootDir in dirsToScan) {
            if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V2) aborted by user")
            val rootFile = File(rootDir)
            if (!rootFile.exists() || !rootFile.isDirectory) continue

            rootFile.walkTopDown().forEach { file ->
                if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V2) aborted by user")
                if (file.isDirectory && file != rootFile) {
                    val mystring = file.name.lowercase().trim()
                    val first = mystring.replace(Regex("[^A-Za-z0-9]+"), "")

                    if (checkRe(first, excludeChapterRe) || checkRe(mystring, excludeChapterRe)) return@forEach

                    val second = file.absolutePath
                    tempStringBuilder.append("$first ::: $second\n")
                    count++
                    onProgress(Progress(count, first))

                    val mystring2 = file.name
                    if (mystring2.isBlank()) return@forEach

                    val result = extractBrackets(mystring2)
                    val baseRow = listOf(
                        mystring2, second,
                        result.extractedName,
                        result.circleList.toString(),
                        result.curlyList.toString(),
                        result.equalList.toString(),
                        result.authorList.toString(),
                        result.tagList.toString(),
                        result.mainTitles.toString(),
                        result.mainTitlesPressed.toString(),
                        result.namePressed
                    ) // 11 fields

                    // Get ComicInfo data
                    var xmlItems = List(9) { "" }
                    val xmlFile = getComicInfoFile(file)
                    if (xmlFile != null) {
                        xmlItems = parseComicInfo(xmlFile)
                    }
                    if (xmlItems.size != 9) {
                        xmlItems = (xmlItems + List(9) { "" }).take(9)
                    }

                    // Get file stats
                    val (totalSize, fileCount, avgSize) = getFilesSizeAndCountAvg(file)
                    val listCount = file.listFiles()?.size ?: 0
                    val hasFolders = checkDirectoryExists(file)

                    val fullRow = baseRow + xmlItems + listOf(
                        listCount.toString(),
                        hasFolders.toString(),
                        totalSize,
                        fileCount.toString(),
                        avgSize
                    ) // 25 fields total
                    finalList.add(fullRow)
                }
            }
        }

        // Scan directories for files
        val fileDirsToScan = resolveReadDirectories(filesDirs)
        for (rootDir in fileDirsToScan) {
            if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V2) aborted by user")
            val rootFile = File(rootDir)
            if (!rootFile.exists() || !rootFile.isDirectory) continue

            rootFile.walkTopDown().forEach { file ->
                if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V2) aborted by user")
                if (file.isFile) {
                    val mystring = file.name.lowercase().trim()
                    val first = mystring.replace(Regex("[^A-Za-z0-9]+"), "")
                    val nameNoExt = file.nameWithoutExtension.lowercase().trim()
                    val firstNoExt = nameNoExt.replace(Regex("[^A-Za-z0-9]+"), "")

                    if (checkRe(first, excludeChapterRe) ||
                        checkRe(mystring, excludeChapterRe) ||
                        checkRe(nameNoExt, excludeChapterRe) ||
                        checkRe(firstNoExt, excludeChapterRe)) return@forEach

                    val second = file.absolutePath
                    tempStringBuilder.append("$first ::: $second\n")
                    count++
                    onProgress(Progress(count, first))

                    val mystring2 = file.name
                    if (mystring2.isBlank()) return@forEach

                    val result = extractBrackets(mystring2)
                    val baseRow = listOf(
                        mystring2, second,
                        result.extractedName,
                        result.circleList.toString(),
                        result.curlyList.toString(),
                        result.equalList.toString(),
                        result.authorList.toString(),
                        result.tagList.toString(),
                        result.mainTitles.toString(),
                        result.mainTitlesPressed.toString(),
                        result.namePressed
                    ) // 11 fields

                    val xmlItems = List(9) { "" }
                    val fileSize = file.length()
                    val readableSize = convertBytesToReadableSize(fileSize)

                    val fullRow = baseRow + xmlItems + listOf(
                        "1",
                        "false",
                        readableSize,
                        "1",
                        readableSize
                    ) // 25 fields total
                    finalList.add(fullRow)
                }
            }
        }

        if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V2) aborted by user")

        val tempListFile = File(outputDir, "list.txt.tmp")
        val tempCountFile = File(outputDir, "count.txt.tmp")
        val tempCsvFile = File(outputDir, "filename_list_v2.csv.tmp")

        try {
            // Write to temporary files first
            tempListFile.writeText(tempStringBuilder.toString(), Charsets.UTF_8)
            tempCountFile.writeText(count.toString(), Charsets.UTF_8)

            BufferedWriter(FileWriter(tempCsvFile, Charsets.UTF_8)).use { writer ->
                for (row in finalList) {
                    if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V2) aborted by user")
                    writer.write(row.joinToString(LIST_CSV_DELIMITER.toString()))
                    writer.newLine()
                }
            }

            if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V2) aborted by user")

            // Atomically replace target files only when completed
            safeAtomicReplace(tempListFile, File(outputDir, "list.txt"))
            safeAtomicReplace(tempCountFile, File(outputDir, "count.txt"))
            safeAtomicReplace(tempCsvFile, File(outputDir, "filename_list_v2.csv"))
        } catch (t: Throwable) {
            tempListFile.delete()
            tempCountFile.delete()
            tempCsvFile.delete()
            throw t
        }

        onProgress(Progress(count, "Done", done = true))
        return count
    }
}
