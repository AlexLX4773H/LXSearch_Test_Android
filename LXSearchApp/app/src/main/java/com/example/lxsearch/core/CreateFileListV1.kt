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
        foldersDirs: List<String> = READ_ROOT_DIR_FOR_FOLDERS,
        filesDirs: List<String> = READ_ROOT_DIR_FOR_FILES,
        smbFoldersDirs: List<String> = SMB_READ_ROOT_DIR_FOR_FOLDERS,
        smbSharePath: String = SMB_SHARE_PATH,
        smbUsername: String = SMB_USERNAME,
        smbPassword: String = SMB_PASSWORD,
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
        val finalList = mutableListOf<List<String>>()
        finalList.add(LIST_CSV_HEADERS)

        // Scan directories for folders
        val dirsToScan = resolveReadDirectories(foldersDirs)
        for (rootDir in dirsToScan) {
            if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V1) aborted by user")
            val rootFile = File(rootDir)
            if (!rootFile.exists() || !rootFile.isDirectory) continue

            rootFile.walkTopDown().forEach { file ->
                if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V1) aborted by user")
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

        // Scan SMB directories for folders and archive files
        if (smbFoldersDirs.isNotEmpty()) {
            val smbFolders = SmbScanner.scanFolders(
                locations = smbFoldersDirs,
                sharePath = smbSharePath,
                username = smbUsername,
                password = smbPassword,
                isCancelled = isCancelled
            )
            for (smbFolder in smbFolders) {
                if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V1) aborted by user")
                val mystring = smbFolder.name.lowercase().trim()
                val first = mystring.replace(Regex("[^A-Za-z0-9]+"), "")

                if (smbFolder.isDirectory) {
                    if (checkRe(first, excludeChapterRe) || checkRe(mystring, excludeChapterRe)) continue
                } else {
                    val nameNoExt = if (smbFolder.name.contains('.')) smbFolder.name.substringBeforeLast('.').lowercase().trim() else mystring
                    val firstNoExt = nameNoExt.replace(Regex("[^A-Za-z0-9]+"), "")
                    if (checkRe(first, excludeChapterRe) ||
                        checkRe(mystring, excludeChapterRe) ||
                        checkRe(nameNoExt, excludeChapterRe) ||
                        checkRe(firstNoExt, excludeChapterRe)) continue
                }

                val second = smbFolder.uncPath
                tempStringBuilder.append("$first ::: $second\n")
                count++
                onProgress(Progress(count, first))

                val mystring2 = smbFolder.name
                if (mystring2.isBlank()) continue

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

        // Scan directories for files
        val fileDirsToScan = resolveReadDirectories(filesDirs)
        for (rootDir in fileDirsToScan) {
            if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V1) aborted by user")
            val rootFile = File(rootDir)
            if (!rootFile.exists() || !rootFile.isDirectory) continue

            rootFile.walkTopDown().forEach { file ->
                if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V1) aborted by user")
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

        if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V1) aborted by user")

        val tempListFile = File(outputDir, "list.txt.tmp")
        val tempCountFile = File(outputDir, "count.txt.tmp")
        val tempCsvFile = File(outputDir, "filename_list.csv.tmp")

        try {
            // Write to temporary files first
            tempListFile.writeText(tempStringBuilder.toString(), Charsets.UTF_8)
            tempCountFile.writeText(count.toString(), Charsets.UTF_8)

            BufferedWriter(FileWriter(tempCsvFile, Charsets.UTF_8)).use { writer ->
                for (row in finalList) {
                    if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V1) aborted by user")
                    writer.write(row.joinToString(LIST_CSV_DELIMITER.toString()))
                    writer.newLine()
                }
            }

            if (isCancelled()) throw java.util.concurrent.CancellationException("Create File List (V1) aborted by user")

            // Atomically replace target files only when completed
            safeAtomicReplace(tempListFile, File(outputDir, "list.txt"))
            safeAtomicReplace(tempCountFile, File(outputDir, "count.txt"))
            safeAtomicReplace(tempCsvFile, File(outputDir, "filename_list.csv"))
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
