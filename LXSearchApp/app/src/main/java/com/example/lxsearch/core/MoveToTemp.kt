package com.example.lxsearch.core

import java.io.File

/**
 * Port of Move_To_Temp_standalone.py
 * Finds empty/incomplete folders and moves them to temp/cleanup destinations.
 * Two-phase: scan (preview) then execute (move/remove) with confirmation.
 */
object MoveToTemp {

    data class MoveInfo(val originalPath: String, val newPath: String)
    data class ScanResult(
        val moveItems: Map<String, MoveInfo>,
        val emptyFolders: Map<String, String>,
        val logs: List<String>
    )

    /** Checks if a folder's total content is below min_empty_size threshold. */
    private fun isEmptyFolder(path: File): Boolean {
        var totalSize = 0L
        path.walkTopDown().forEach { file ->
            if (file.isFile) {
                totalSize += file.length()
                if (totalSize > MIN_EMPTY_SIZE) return false
            }
        }
        return true
    }

    /**
     * Checks if valid image files start numbering above min_page_no.
     * Indicates an incomplete/continuing download.
     */
    private fun isIncompleteContinuation(path: File): Boolean {
        val fileNumbers = mutableListOf<Int>()
        path.walkTopDown().forEach { file ->
            if (file.isFile && ".${file.extension.lowercase()}" in V2_VALID_EXT) {
                val nums = extractNumbers(file.name)
                fileNumbers.addAll(nums.mapNotNull { it.toIntOrNull() })
            }
        }
        val uniqueSorted = fileNumbers.toSortedSet().toList()
        return uniqueSorted.isNotEmpty() && uniqueSorted.first() > MIN_PAGE_NO
    }

    /** Checks if folder name does NOT match any exception patterns. */
    private fun isValidFolder(name: String): Boolean {
        return checkValid(name, EXCEPTION_LIST_FOLDER_EMPTY)
    }

    /**
     * Scans all configured source directories and identifies items to move.
     * Does NOT perform any moves — preview only.
     * @return ScanResult with items to move and empty folders to delete
     */
    fun scan(): ScanResult {
        val moveItems = mutableMapOf<String, MoveInfo>()
        val emptyFolders = mutableMapOf<String, String>()
        val logs = mutableListOf<String>()

        // Phase 1: Scan main source for empty subfolders
        val listSourceFile = File(MOVE_LIST_SOURCE)
        if (listSourceFile.exists()) {
            val seriesDirs = listSourceFile.listFiles()?.filter { it.isDirectory } ?: emptyList()
            for (seriesDir in seriesDirs) {
                val name = seriesDir.name
                if (!isValidFolder(name)) continue

                val bookDirs = seriesDir.listFiles()?.filter { it.isDirectory } ?: continue
                for (bookDir in bookDirs) {
                    val bookName = bookDir.name
                    if (isEmptyFolder(bookDir)) {
                        val targetDest = "$MOVE_LIST_DEST${File.separator}$name${File.separator}$bookName"
                        moveItems[bookName] = MoveInfo(bookDir.absolutePath, targetDest)
                        logs.add("EMPTY: ${bookDir.absolutePath}\n → $targetDest")
                    }
                }
            }
        }

        // Phase 2: Scan zzz sources for empty and incomplete
        for (rootDir in MOVE_SOURCE_ZZZ) {
            val rootFile = File(rootDir)
            if (!rootFile.exists()) continue

            val dirs = rootFile.listFiles()?.filter { it.isDirectory } ?: continue
            for (dir in dirs) {
                val dirName = dir.name
                if (isEmptyFolder(dir)) {
                    val targetDest = "$MOVE_DEST_ZZZ${File.separator}$dirName"
                    moveItems[dirName] = MoveInfo(dir.absolutePath, targetDest)
                    logs.add("ZZZ EMPTY: ${dir.absolutePath}\n → $targetDest")
                } else if (isIncompleteContinuation(dir)) {
                    val targetDest = "$MOVE_DEST_ZZZ_INC${File.separator}$dirName"
                    moveItems[dirName] = MoveInfo(dir.absolutePath, targetDest)
                    logs.add("ZZZ INC: ${dir.absolutePath}\n → $targetDest")
                }
            }
        }

        // Phase 3: Scan S sources for empty
        for (rootDir in MOVE_SOURCE_S) {
            val rootFile = File(rootDir)
            if (!rootFile.exists()) continue

            val dirs = rootFile.listFiles()?.filter { it.isDirectory } ?: continue
            for (dir in dirs) {
                val dirName = dir.name
                if (isEmptyFolder(dir)) {
                    val targetDest = "$MOVE_DEST_S${File.separator}$dirName"
                    moveItems[dirName] = MoveInfo(dir.absolutePath, targetDest)
                    logs.add("S EMPTY: ${dir.absolutePath}\n → $targetDest")
                }
            }
        }

        // Phase 4: Find empty parent folders in main source (for deletion)
        if (listSourceFile.exists()) {
            val seriesDirs = listSourceFile.listFiles()?.filter { it.isDirectory } ?: emptyList()
            for (seriesDir in seriesDirs) {
                val name = seriesDir.name
                if (!isValidFolder(name)) continue
                if (isEmptyFolder(seriesDir)) {
                    emptyFolders[name] = seriesDir.absolutePath
                    logs.add("EMPTY PARENT: $name")
                }
            }
        }

        logs.add("Total items to move: ${moveItems.size}")
        logs.add("Total empty folders to remove: ${emptyFolders.size}")

        return ScanResult(moveItems, emptyFolders, logs)
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

    /**
     * Removes empty directories.
     * @return List of log messages
     */
    fun executeRemove(folders: Map<String, String>): List<String> {
        val logs = mutableListOf<String>()
        var removedCount = 0

        for ((name, path) in folders) {
            try {
                val dir = File(path)
                if (dir.delete()) {
                    logs.add("Removed: $name")
                    removedCount++
                } else {
                    logs.add("Cannot remove: $name")
                }
            } catch (e: Exception) {
                logs.add("ERROR removing $name: ${e.message}")
            }
        }

        logs.add("Removed $removedCount of ${folders.size} folders.")
        return logs
    }
}
