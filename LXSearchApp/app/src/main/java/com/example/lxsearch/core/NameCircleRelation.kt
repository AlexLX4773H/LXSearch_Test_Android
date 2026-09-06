package com.example.lxsearch.core

import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.StringReader

/**
 * Port of Make_Name_Circle_relation_standalone.py
 * Scans series folders, extracts circle/parody names from folder brackets
 * and ComicInfo, builds name→parody mapping, outputs JSON/CSV/duplicate report.
 */
object NameCircleRelation {

    data class Progress(val message: String, val done: Boolean = false)
    data class Result(val seriesCount: Int, val duplicateCount: Int)

    private val parodyPat = Regex("""^\s*(?:\*\*)?\s*parod(?:ies|y)\s*(?:\*\*)?\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)

    /**
     * Extracts circle bracket parody from folder name.
     * Square brackets are removed first to avoid author leakage.
     */
    private fun extractBracketsParody(
        folderName: String,
        excludeBrackets: Set<String>,
        excludeBracketsRe: List<String>
    ): List<String> {
        // Remove square brackets
        var withoutSquares = folderName
        PATTERN_SQUARE_FULL.findAll(folderName).forEach {
            withoutSquares = withoutSquares.replace(it.groupValues[1], "")
        }

        // Extract circle brackets
        val circleItems = PATTERN_CIRCLE.findAll(withoutSquares)
            .map { it.groupValues[1].trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toList()

        // Filter using exclusions
        val validItems = circleItems.filter { item ->
            item !in excludeBrackets && checkValid(item, excludeBracketsRe)
        }

        // Return last valid circle bracket (original convention)
        return if (validItems.isNotEmpty()) listOf(validItems.last()) else emptyList()
    }

    /** Finds ComicInfo file in folder or immediate subdirectories. */
    private fun getComicInfoFile(folderPath: File): File? {
        try {
            if (!folderPath.exists() || !folderPath.isDirectory) return null
            for (f in folderPath.listFiles() ?: emptyArray()) {
                if (f.name.lowercase() in VALID_XML_NAMES) return f
            }
            for (sub in folderPath.listFiles() ?: emptyArray()) {
                if (sub.isDirectory) {
                    for (subFile in sub.listFiles() ?: emptyArray()) {
                        if (subFile.name.lowercase() in VALID_XML_NAMES) return subFile
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /** Extracts parody from ComicInfo file. */
    private fun extractComicInfoParody(
        folderPath: File,
        excludeBrackets: Set<String>,
        excludeBracketsRe: List<String>
    ): List<String> {
        val xmlFile = getComicInfoFile(folderPath) ?: return emptyList()
        var parodyRaw = ""

        try {
            val content = xmlFile.readText(Charsets.UTF_8)

            if (xmlFile.name.lowercase().endsWith(".json")) {
                val json = JSONObject(content)
                parodyRaw = json.optString("parodies", "") .ifEmpty { json.optString("parody", "") }
            } else {
                // Parse XML
                try {
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(StringReader(content))
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if (eventType == XmlPullParser.START_TAG) {
                            val tagName = parser.name.lowercase().let {
                                if ('}' in it) it.substringAfter('}') else it
                            }
                            if (tagName in listOf("parodies", "parody")) {
                                parodyRaw = parser.nextText()?.trim() ?: ""
                                if (parodyRaw.isNotEmpty()) break
                            }
                        }
                        eventType = parser.next()
                    }
                } catch (_: Exception) {}

                // Fallback to summary
                if (parodyRaw.isEmpty()) {
                    try {
                        val factory = XmlPullParserFactory.newInstance()
                        val parser = factory.newPullParser()
                        parser.setInput(StringReader(content))
                        var eventType = parser.eventType
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            if (eventType == XmlPullParser.START_TAG) {
                                val tagName = parser.name.lowercase()
                                if (tagName in listOf("summary", "description", "notes")) {
                                    val summaryText = parser.nextText()?.trim() ?: ""
                                    for (line in summaryText.lines()) {
                                        val m = parodyPat.find(line.trim())
                                        if (m != null) {
                                            parodyRaw = m.groupValues[1].trim()
                                            break
                                        }
                                    }
                                    if (parodyRaw.isNotEmpty()) break
                                }
                            }
                            eventType = parser.next()
                        }
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {
            return emptyList()
        }

        if (parodyRaw.isEmpty()) return emptyList()

        // Split parodies by |, ,, /, or ;
        return parodyRaw.split(Regex("[,|/;]+"))
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() && it !in excludeBrackets && checkValid(it, excludeBracketsRe) }
    }

    /** Loads exclusion lists from input folder. */
    private fun loadExclusions(inputDir: File): Pair<Set<String>, List<String>> {
        val excludeBrackets = mutableSetOf<String>()
        val excludeBracketsRe = mutableListOf<String>()

        val fileTxt = File(inputDir, "exclude_in_brackets.txt")
        if (fileTxt.exists()) {
            fileTxt.readLines(Charsets.UTF_8).forEach { line ->
                val s = line.trim().lowercase()
                if (s.isNotEmpty()) excludeBrackets.add(s)
            }
        }

        val fileRe = File(inputDir, "exclude_in_brackets_re.txt")
        if (fileRe.exists()) {
            fileRe.readLines(Charsets.UTF_8).forEach { line ->
                val s = line.trim()
                if (s.isNotEmpty()) excludeBracketsRe.add(s)
            }
        }

        return Pair(excludeBrackets, excludeBracketsRe)
    }

    /**
     * Runs the name-circle relation building process.
     * @param outputDir The directory to write output files to
     * @param inputDir The directory containing exclusion text files
     * @param rootDirs Optional list of root directories to scan (defaults to DEFAULT_ROOT_DIRS)
     * @param onProgress Callback for progress updates
     * @return Result with series count and duplicate count
     */
    fun run(
        outputDir: File,
        inputDir: File,
        rootDirs: List<String>? = null,
        onProgress: (Progress) -> Unit = {}
    ): Result {
        val dirsToScan = rootDirs ?: DEFAULT_ROOT_DIRS.filter { File(it).exists() }.ifEmpty { DEFAULT_ROOT_DIRS }
        outputDir.mkdirs()

        val (excludeBrackets, excludeBracketsRe) = loadExclusions(inputDir)
        onProgress(Progress("Reading Data..."))

        val outputDict = mutableMapOf<String, MutableList<String>>()

        for (rootDir in dirsToScan) {
            val rootFile = File(rootDir)
            if (!rootFile.exists()) {
                onProgress(Progress("Directory not found: $rootDir"))
                continue
            }
            onProgress(Progress("Scanning: $rootDir"))

            val seriesFolders = rootFile.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name } ?: continue

            for (seriesDir in seriesFolders) {
                // Skip exception folders
                if (!checkValid(seriesDir.name, EXCEPTION_LIST_FOLDER_START)) continue

                val seriesElements = mutableListOf<String>()

                val bookFolders = seriesDir.listFiles()?.filter { it.isDirectory } ?: continue
                for (bookDir in bookFolders) {
                    // Extract from brackets in folder name
                    seriesElements.addAll(extractBracketsParody(bookDir.name, excludeBrackets, excludeBracketsRe))
                    // Extract from ComicInfo
                    seriesElements.addAll(extractComicInfoParody(bookDir, excludeBrackets, excludeBracketsRe))
                }

                val deduped = deduplicateList(seriesElements).toMutableList()
                if (seriesDir.name in outputDict) {
                    outputDict[seriesDir.name] = deduplicateList(outputDict[seriesDir.name]!! + deduped).toMutableList()
                } else {
                    outputDict[seriesDir.name] = deduped
                }
            }
        }

        onProgress(Progress("Writing Data..."))

        // 1. JSON output
        val jsonArray = JSONArray()
        for ((name, list) in outputDict) {
            val obj = JSONObject()
            obj.put("Name", name)
            obj.put("List", JSONArray(list))
            jsonArray.put(obj)
        }
        File(outputDir, "output_list_name.json").writeText(jsonArray.toString(4), Charsets.UTF_8)

        // 2. Duplicate detection
        var countDup = 0
        val dupBuilder = StringBuilder()
        val fillListTmp = mutableSetOf<String>()
        for ((_, items) in outputDict) {
            val intersection = fillListTmp.intersect(items.toSet())
            if (intersection.isNotEmpty()) {
                for (i in intersection) {
                    dupBuilder.appendLine(i)
                    countDup++
                }
            }
            fillListTmp.addAll(items)
        }
        File(outputDir, "output_duplicate.txt").writeText(dupBuilder.toString(), Charsets.UTF_8)

        // 3. CSV output
        BufferedWriter(FileWriter(File(outputDir, "output_list_name.csv"), Charsets.UTF_8)).use { writer ->
            writer.write("Name${LIST_CSV_DELIMITER}Element")
            writer.newLine()
            for ((name, items) in outputDict) {
                for (item in items) {
                    writer.write("$name${LIST_CSV_DELIMITER}$item")
                    writer.newLine()
                }
            }
        }

        val result = Result(outputDict.size, countDup)
        onProgress(Progress("Done! Processed ${outputDict.size} series. Duplicates: $countDup", done = true))
        return result
    }
}
