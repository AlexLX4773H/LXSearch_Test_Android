package com.example.lxsearch.core

import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import java.util.zip.ZipFile

/**
 * Port of Move_From_Source_standalone.py
 * Matches source releases to series folders using bracket extraction + ComicInfo +
 * JSON mapping, then moves them. Two-phase: scan (match/preview) then execute (move).
 */
object MoveFromSource {

    data class MoveInfo(
        val originalPath: String,
        val newPath: String,
        val series: String,
        val matchedToken: String,
        val source: String
    )

    data class ScanResult(
        val items: Map<String, MoveInfo>,
        val logs: List<String>
    )

    private val PARODIES_PAT = Regex("""^\s*(?:\*\*)?\s*parod(?:ies|y)\s*(?:\*\*)?\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)
    private val SERIES_PAT = Regex("""^\s*(?:\*\*)?\s*series\s*(?:\*\*)?\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)
    private val GROUPS_PAT = Regex("""^\s*(?:\*\*)?\s*(?:groups?|circles?)\s*(?:\*\*)?\s*:\s*(.+)$""", RegexOption.IGNORE_CASE)

    // ── Exclusion Loading ──

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

    // ── JSON Mapping Loading ──

    private fun loadJsonMapping(jsonPath: File): Pair<List<JSONObject>, Map<String, String>> {
        if (!jsonPath.exists()) return Pair(emptyList(), emptyMap())

        try {
            val content = jsonPath.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(content)
            val listDictData = mutableListOf<JSONObject>()
            val mapping = mutableMapOf<String, String>()

            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                listDictData.add(item)

                val canonicalName = item.optString("Name", "")
                if (canonicalName.isEmpty()) continue

                val nameLower = canonicalName.trim().lowercase()
                if (nameLower !in mapping) mapping[nameLower] = canonicalName

                val list = item.optJSONArray("List") ?: continue
                for (j in 0 until list.length()) {
                    val alias = list.optString(j, "").trim().lowercase()
                    if (alias.isNotEmpty() && alias !in mapping) {
                        mapping[alias] = canonicalName
                    }
                }
            }

            return Pair(listDictData, mapping)
        } catch (_: Exception) {}

        return Pair(emptyList(), emptyMap())
    }

    // ── Bracket Extraction ──

    private fun extractFolderCircles(
        folderName: String,
        excludeBrackets: Set<String>,
        excludeBracketsRe: List<String>
    ): List<String> {
        var withoutSquares = folderName
        PATTERN_SQUARE_FULL.findAll(folderName).forEach {
            withoutSquares = withoutSquares.replace(it.groupValues[1], "")
        }

        val circleItems = PATTERN_CIRCLE.findAll(withoutSquares)
            .map { it.groupValues[1].trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toList()

        val filtered = circleItems.filter { item ->
            item !in excludeBrackets && checkValid(item, excludeBracketsRe)
        }

        return deduplicateList(filtered).reversed()
    }

    // ── ComicInfo Extraction ──

    private fun parseComicInfoContent(content: String, isJson: Boolean): Triple<String, String, String> {
        var parodyRaw = ""
        var seriesRaw = ""
        var groupRaw = ""

        try {
            if (isJson) {
                val json = JSONObject(content)
                parodyRaw = json.optString("parodies", "").ifEmpty { json.optString("parody", "") }
                seriesRaw = json.optString("series", "")
                groupRaw = json.optString("groups", "").ifEmpty {
                    json.optString("group", "").ifEmpty {
                        json.optString("circle", "").ifEmpty {
                            json.optString("circles", "")
                        }
                    }
                }
                val summary = json.optString("summary", "").ifEmpty {
                    json.optString("description", "").ifEmpty {
                        json.optString("notes", "")
                    }
                }
                if (summary.isNotEmpty()) {
                    parseSummaryLines(summary) { p, s, g ->
                        if (parodyRaw.isEmpty()) parodyRaw = p
                        if (seriesRaw.isEmpty()) seriesRaw = s
                        if (groupRaw.isEmpty()) groupRaw = g
                    }
                }
            } else {
                // Parse XML
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(StringReader(content))
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        val tagName = parser.name?.lowercase()?.let {
                            if ('}' in it) it.substringAfter('}') else it
                        } ?: ""
                        when {
                            tagName in listOf("parodies", "parody") && parodyRaw.isEmpty() ->
                                parodyRaw = parser.nextText()?.trim() ?: ""
                            tagName == "series" && seriesRaw.isEmpty() ->
                                seriesRaw = parser.nextText()?.trim() ?: ""
                            tagName in listOf("groups", "group", "circle", "circles", "teams", "team") && groupRaw.isEmpty() ->
                                groupRaw = parser.nextText()?.trim() ?: ""
                            tagName in listOf("summary", "description", "notes") -> {
                                val text = parser.nextText()?.trim() ?: ""
                                if (text.isNotEmpty()) {
                                    parseSummaryLines(text) { p, s, g ->
                                        if (parodyRaw.isEmpty()) parodyRaw = p
                                        if (seriesRaw.isEmpty()) seriesRaw = s
                                        if (groupRaw.isEmpty()) groupRaw = g
                                    }
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (_: Exception) {}

        return Triple(parodyRaw, seriesRaw, groupRaw)
    }

    private fun parseSummaryLines(text: String, onMatch: (String, String, String) -> Unit) {
        var p = ""
        var s = ""
        var g = ""
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (p.isEmpty()) PARODIES_PAT.find(trimmed)?.let { p = it.groupValues[1].trim() }
            if (s.isEmpty()) SERIES_PAT.find(trimmed)?.let { s = it.groupValues[1].trim() }
            if (g.isEmpty()) GROUPS_PAT.find(trimmed)?.let { g = it.groupValues[1].trim() }
        }
        onMatch(p, s, g)
    }

    private fun getComicInfoFromFolder(folderPath: File): Pair<String?, Boolean> {
        try {
            if (!folderPath.exists() || !folderPath.isDirectory) return Pair(null, false)

            for (f in folderPath.listFiles() ?: emptyArray()) {
                if (f.name.lowercase() in VALID_XML_NAMES) {
                    return Pair(f.readText(Charsets.UTF_8), f.name.lowercase().endsWith(".json"))
                }
            }

            for (sub in folderPath.listFiles() ?: emptyArray()) {
                if (sub.isDirectory) {
                    for (subFile in sub.listFiles() ?: emptyArray()) {
                        if (subFile.name.lowercase() in VALID_XML_NAMES) {
                            return Pair(subFile.readText(Charsets.UTF_8), subFile.name.lowercase().endsWith(".json"))
                        }
                    }
                }
            }

            // Check CBZ/ZIP archives
            for (f in folderPath.listFiles() ?: emptyArray()) {
                if (f.name.lowercase().endsWith(".cbz") || f.name.lowercase().endsWith(".zip")) {
                    val result = getComicInfoFromArchive(f)
                    if (result.first != null) return result
                }
            }
        } catch (_: Exception) {}

        return Pair(null, false)
    }

    private fun getComicInfoFromArchive(archivePath: File): Pair<String?, Boolean> {
        try {
            if (!archivePath.isFile) return Pair(null, false)
            ZipFile(archivePath).use { zf ->
                for (entry in zf.entries()) {
                    val baseLower = File(entry.name).name.lowercase()
                    if (baseLower in VALID_XML_NAMES) {
                        val content = zf.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
                        return Pair(content, baseLower.endsWith(".json"))
                    }
                }
            }
        } catch (_: Exception) {}
        return Pair(null, false)
    }

    private fun extractComicInfoCandidates(
        path: File,
        excludeBrackets: Set<String>,
        excludeBracketsRe: List<String>
    ): List<String> {
        val (content, isJson) = if (path.isFile) {
            getComicInfoFromArchive(path)
        } else {
            getComicInfoFromFolder(path)
        }

        if (content == null) return emptyList()

        val (parodyRaw, seriesRaw, groupRaw) = parseComicInfoContent(content, isJson)

        val candidates = mutableListOf<String>()
        for (rawStr in listOf(parodyRaw, seriesRaw, groupRaw)) {
            if (rawStr.isEmpty()) continue
            rawStr.split(Regex("[,|/;]+")).forEach { part ->
                val clean = part.trim().lowercase()
                if (clean.isNotEmpty() && clean !in excludeBrackets && checkValid(clean, excludeBracketsRe)) {
                    candidates.add(clean)
                }
            }
        }

        return deduplicateList(candidates)
    }

    // ── Matching Logic ──

    private fun findMatch(
        folderName: String,
        fullPath: File,
        excludeBrackets: Set<String>,
        excludeBracketsRe: List<String>,
        listDictData: List<JSONObject>,
        mapping: Map<String, String>
    ): Triple<String?, String?, String?> {
        val bracketItems = extractFolderCircles(folderName, excludeBrackets, excludeBracketsRe)
        val comicInfoItems = extractComicInfoCandidates(fullPath, excludeBrackets, excludeBracketsRe)

        data class Candidate(val token: String, val source: String)

        val candidates = mutableListOf<Candidate>()
        for (item in bracketItems) candidates.add(Candidate(item, "Folder Bracket"))
        for (item in comicInfoItems) {
            if (candidates.none { it.token == item }) {
                candidates.add(Candidate(item, "ComicInfo"))
            }
        }

        for (candidate in candidates) {
            // Fast hash lookup
            val mapped = mapping[candidate.token]
            if (mapped != null) return Triple(mapped, candidate.token, candidate.source)

            // Fallback direct list search
            for (jsonItem in listDictData) {
                val list = jsonItem.optJSONArray("List") ?: continue
                for (i in 0 until list.length()) {
                    if (candidate.token == list.optString(i, "")) {
                        return Triple(jsonItem.optString("Name"), candidate.token, candidate.source)
                    }
                }
            }
        }

        return Triple(null, null, null)
    }

    /**
     * Scans source directories and matches releases to series destinations.
     * Does NOT perform any moves — preview only.
     * @param outputDir Directory containing output_list_name.json
     * @param inputDir Directory containing exclusion text files
     * @return ScanResult with matched items and logs
     */
    fun scan(outputDir: File, inputDir: File): ScanResult {
        val logs = mutableListOf<String>()
        val destDir = CANDIDATE_DESTINATIONS.firstOrNull { File(it).exists() } ?: CANDIDATE_DESTINATIONS.first()
        val candidateSources = resolveReadDirectories(CANDIDATE_SOURCES)
        val sourceDirs = candidateSources.filter { File(it).exists() }.ifEmpty { candidateSources }

        logs.add("Destination: $destDir")
        logs.add("Sources to scan (${sourceDirs.size}):")
        for (s in sourceDirs) {
            val status = if (File(s).exists()) "exists" else "not found"
            logs.add("  - $s [$status]")
        }

        val (excludeBrackets, excludeBracketsRe) = loadExclusions(inputDir)
        val jsonPath = File(outputDir, "output_list_name.json")
        val (listDictData, mapping) = loadJsonMapping(jsonPath)

        if (mapping.isEmpty() && listDictData.isEmpty()) {
            logs.add("Error: No valid mapping data found.")
            return ScanResult(emptyMap(), logs)
        }

        val dictOutput = mutableMapOf<String, MoveInfo>()

        for (rootDir in sourceDirs) {
            val rootFile = File(rootDir)
            if (!rootFile.exists()) continue

            val entries = rootFile.listFiles()?.sortedBy { it.name } ?: continue

            for (entry in entries) {
                val entryName = entry.name
                if (entryName.isBlank()) continue

                val (matchedName, matchedToken, matchSource) = findMatch(
                    entryName, entry, excludeBrackets, excludeBracketsRe, listDictData, mapping
                )

                if (matchedName == null) continue

                val targetPath = "$destDir${File.separator}$matchedName${File.separator}$entryName"
                dictOutput[entryName] = MoveInfo(
                    entry.absolutePath, targetPath, matchedName,
                    matchedToken ?: "", matchSource ?: ""
                )

                logs.add("$entryName → $matchedName [$matchSource: '$matchedToken']")
            }
        }

        logs.add("Total matching items: ${dictOutput.size}")
        return ScanResult(dictOutput, logs)
    }

    /**
     * Executes the move operation for the given items.
     * @return List of log messages
     */
    fun executeMove(items: Map<String, MoveInfo>): List<String> {
        val logs = mutableListOf<String>()
        var movedCount = 0

        for ((name, info) in items) {
            try {
                val source = File(info.originalPath)
                val target = File(info.newPath)
                target.parentFile?.mkdirs()

                logs.add("MOVING: $name")
                logs.add("  FROM: ${info.originalPath}")
                logs.add("  TO:   ${info.newPath}")

                source.renameTo(target)
                movedCount++
            } catch (e: Exception) {
                logs.add("ERROR moving $name: ${e.message}")
            }
        }

        logs.add("Moved $movedCount of ${items.size} items.")
        return logs
    }
}
