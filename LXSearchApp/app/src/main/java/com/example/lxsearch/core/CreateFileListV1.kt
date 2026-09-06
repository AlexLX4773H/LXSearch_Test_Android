package com.example.lxsearch.core

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

/**
 * Port of Run_Create_file_list_v1_standalone.py
 * Scans configured directories for folders/files, extracts bracket metadata,
 * writes filename_list.csv, list.txt, count.txt to the output directory.
 */
object CreateFileListV1 {

    data class Progress(val current: Int, val name: String, val done: Boolean = false)

    /**
     * Runs the file list creation process (v1).
     * @param outputDir The directory to write output files to
     * @param inputDir The directory containing exclusion text files
     * @param onProgress Callback for progress updates
     * @return Total count of processed items
     */
    fun run(
        outputDir: File,
        inputDir: File,
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
        val finalList = mutableListOf<List<String>>()
        finalList.add(LIST_CSV_HEADERS)

        // Scan directories for folders
        val dirsToScan = resolveReadDirectories(READ_ROOT_DIR_FOR_FOLDERS)
        for (rootDir in dirsToScan) {
            val rootFile = File(rootDir)
            if (!rootFile.exists() || !rootFile.isDirectory) continue

            rootFile.walkTopDown().forEach { file ->
                if (file.isDirectory && file != rootFile) {
                    val mystring = file.name.lowercase().trim()
                    val first = mystring.replace(Regex("[^A-Za-z0-9]+"), "")

                    if (checkRe(first, excludeChapterRe)) return@forEach

                    val second = file.absolutePath
                    tempStringBuilder.append("$first ::: $second\n")
                    count++
                    onProgress(Progress(count, first))

                    val mystring2 = file.name
                    if (mystring2.isBlank()) return@forEach

                    val result = extractBrackets(mystring2)
                    val row = listOf(
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
                    )
                    finalList.add(row)
                }
            }
        }

        // Scan directories for files
        for (rootDir in READ_ROOT_DIR_FOR_FILES) {
            val rootFile = File(rootDir)
            if (!rootFile.exists() || !rootFile.isDirectory) continue

            rootFile.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val mystring = file.name.lowercase().trim()
                    val first = mystring.replace(Regex("[^A-Za-z0-9]+"), "")
                    val second = file.absolutePath
                    tempStringBuilder.append("$first ::: $second\n")
                    count++
                    onProgress(Progress(count, first))
                }
            }
        }

        // Write list.txt
        File(outputDir, "list.txt").writeText(tempStringBuilder.toString(), Charsets.UTF_8)

        // Write count.txt
        File(outputDir, "count.txt").writeText(count.toString(), Charsets.UTF_8)

        // Write filename_list.csv
        BufferedWriter(FileWriter(File(outputDir, "filename_list.csv"), Charsets.UTF_8)).use { writer ->
            for (row in finalList) {
                writer.write(row.joinToString(LIST_CSV_DELIMITER.toString()))
                writer.newLine()
            }
        }

        onProgress(Progress(count, "Done", done = true))
        return count
    }
}
