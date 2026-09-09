package com.example.lxsearch.core

import java.io.File

/**
 * Direct port of shared utility functions and constants from the Python scripts.
 * Preserves all regex patterns, CSV delimiters, bracket extraction logic, etc.
 */

// ============================================================
// Regex Patterns
// ============================================================
val PATTERN_SQUARE = Regex("""\[([^\]]+)\]""")
val PATTERN_SQUARE_FULL = Regex("""(\[([^\]]+)\])""")
val PATTERN_CIRCLE = Regex("""\(([^)]+)\)""")
val PATTERN_CIRCLE_FULL = Regex("""(\(([^)]+)\))""")
val PATTERN_CURLY = Regex("""(\{([^}]+)\})""")
val PATTERN_EQUAL = Regex("""(=([^=]+)=)""")

// ============================================================
// Constants
// ============================================================
val INPUT_SEPARATORS = listOf("|", "/", ";")
const val LIST_CSV_DELIMITER = '╥'

val LIST_CSV_HEADERS = listOf(
    "Name", "Folder", "Extracted Name", "Circle List", "Curly List",
    "Equal List", "Author List", "Tag List", "Main Titles",
    "Main Titles Pressed", "Name Pressed"
)

val LIST_CSV_HEADERS_THAT_ARE_LIST = listOf(
    "Circle List", "Curly List", "Equal List", "Author List",
    "Tag List", "Main Titles", "Main Titles Pressed"
)

// V2 specific headers
val V2_LIST_SUMMARY = listOf("Parodies", "Groups", "Characters", "Pages", "Language", "Categories")
val V2_LIST = listOf("Genre", "Writer", "Penciller")
const val V2_SUMMARY = "Summary"
const val V2_XML_INFO = "ComicInfo.xml"
val V2_END_ITEMS = listOf("Count Items", "Has Folders", "Total Size", "Count Files", "Average Size")
val V2_LIST_CSV_HEADERS = LIST_CSV_HEADERS + V2_LIST_SUMMARY + V2_LIST + V2_END_ITEMS // 25 total
val V2_VALID_EXT = listOf(".png", ".jpg", ".jpeg", ".gif", ".webp")
val ARCHIVE_EXTENSIONS = setOf(".cbz", ".zip", ".rar", ".cbr", ".7z", ".cb7", ".tar", ".cbt", ".gz", ".xz")
val IMAGE_EXTENSIONS = setOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".tiff", ".avif")

// Directory paths
const val MIHON_DOWNLOADS_DIR = "/storage/emulated/0/Vere2/Vere/Mihon/downloads"

val DEFAULT_MIHON_HENTAI_FOLDERS = listOf(
    "/storage/emulated/0/Vere2/Vere/Mihon/downloads/NineHentai (EN)",
    "/storage/emulated/0/Vere2/Vere/Mihon/downloads/NHentai (EN)",
    "/storage/emulated/0/Vere2/Vere/Mihon/downloads/nHentai.com (unoriginal) (EN)",
    "/storage/emulated/0/Vere2/Vere/Mihon/downloads/E-Hentai (EN)",
    "/storage/emulated/0/Vere2/Vere/Mihon/downloads/HentaiHand (ALL)",
    "/storage/emulated/0/Vere2/Vere/Mihon/downloads/HentaiHand (EN)",
    "/storage/emulated/0/Vere2/Vere/Mihon/downloads/AsmHentai (EN)",
    "/storage/emulated/0/Vere2/Vere/Mihon/downloads/NHentai.xxx (EN)"
)

val BASE_READ_ROOT_DIR_FOR_FOLDERS = listOf(
    "/storage/emulated/0/Vere2/TempD",
    "/storage/emulated/0/Vere2/TempT",
    "/storage/emulated/0/Vere2/Vere/NewFolder"
)

/**
 * Checks whether a given directory path points to the Mihon downloads folder.
 * Matches with or without leading/trailing slashes, and case-insensitively.
 */
fun isMihonDownloadsDir(path: String): Boolean {
    val normalized = path.trim().trimEnd('/', '\\')
    return normalized.equals("/storage/emulated/0/Vere2/Vere/Mihon/downloads", ignoreCase = true) ||
           normalized.equals("storage/emulated/0/Vere2/Vere/Mihon/downloads", ignoreCase = true)
}

/**
 * Returns all subdirectories within Mihon downloads whose names contain "Hentai" (case-insensitive).
 * If baseDir exists on the filesystem, it queries and sorts all matching subdirectories.
 * Otherwise, falls back to DEFAULT_MIHON_HENTAI_FOLDERS.
 */
fun getMihonHentaiFolders(baseDir: String = MIHON_DOWNLOADS_DIR): List<String> {
    val dir = File(baseDir.trimEnd('/', '\\'))
    if (dir.exists() && dir.isDirectory) {
        val matchingFolders = dir.listFiles { file ->
            file.isDirectory && file.name.contains("Hentai", ignoreCase = true)
        }?.map { it.absolutePath }?.sorted()

        return matchingFolders ?: emptyList()
    }
    return DEFAULT_MIHON_HENTAI_FOLDERS
}

/**
 * Resolves a list of directory paths. Any path pointing to the Mihon downloads folder
 * is expanded into all subfolders whose names contain "Hentai".
 */
fun resolveReadDirectories(directories: List<String>): List<String> {
    val result = mutableListOf<String>()
    for (dir in directories) {
        if (isMihonDownloadsDir(dir)) {
            result.addAll(getMihonHentaiFolders(dir))
        } else {
            result.add(dir)
        }
    }
    return result
}

val READ_ROOT_DIR_FOR_FOLDERS: List<String>
    get() = BASE_READ_ROOT_DIR_FOR_FOLDERS + getMihonHentaiFolders()

val READ_ROOT_DIR_FOR_FILES = listOf(
    "/storage/emulated/0/Vere2/TempT",
    "/storage/emulated/0/Vere2/Vere/NewFolder/Files"
)

// SMB Configuration
const val SMB_SHARE_PATH = """\\192.168.88.234\Share2sgb"""
const val SMB_USERNAME = "alex"
const val SMB_PASSWORD = "aaaaaaaa"
val SMB_READ_ROOT_DIR_FOR_FOLDERS = listOf(
    """\\192.168.88.234\Share2sgb\Manga CBZ\Doujinshi\Archived"""
)

// Move_To_Temp paths
const val MOVE_LIST_SOURCE = "/storage/emulated/0/Vere2/Vere/NewFolder/Vere2"
const val MOVE_LIST_DEST = "/storage/emulated/0/Vere2/TempD"
val MOVE_SOURCE_ZZZ = listOf(
    "/storage/emulated/0/Vere2/Vere/NewFolder/Vere2/000 Zzz",
    "/storage/emulated/0/Vere2/Vere/NewFolder/Vere2/000 Cc"
)
const val MOVE_DEST_ZZZ = "/storage/emulated/0/Vere2/Vere/NewFolder/Vere2/000 Zzz e"
const val MOVE_DEST_ZZZ_INC = "/storage/emulated/0/Vere2/Vere/NewFolder/Vere2/000 CC inc"
val MOVE_SOURCE_S = listOf("/storage/emulated/0/Vere2/Vere/NewFolder/Vere2/000 S")
const val MOVE_DEST_S = "/storage/emulated/0/Vere2/Vere/NewFolder/Vere2/000 S e"

const val MIN_EMPTY_SIZE = 10240L
const val MIN_PAGE_NO = 5

val EXCEPTION_LIST_FOLDER_EMPTY = listOf("^000", "^zzz")

// Move_From_Source paths
val CANDIDATE_DESTINATIONS = listOf(
    "/storage/emulated/0/Vere2/Vere/NewFolder/Vere2"
)

val BASE_CANDIDATE_SOURCES = listOf(
    "/storage/emulated/0/Vere2/Vere/NewFolder/Vere2/000 new",
    "/storage/emulated/0/Vere2/Vere/NewFolder/Vere2/000 S"
)

val CANDIDATE_SOURCES: List<String>
    get() = BASE_CANDIDATE_SOURCES + getMihonHentaiFolders()


// Name-Circle relation paths
val DEFAULT_ROOT_DIRS = listOf(
    "/storage/emulated/0/Vere2/Vere/NewFolder/Vere2",
    "/storage/emulated/0/Vere2/TempD"
)
val EXCEPTION_LIST_FOLDER_START = listOf("""^000""", """^original""", """^zzz""", """^various""")
val VALID_XML_NAMES = setOf("comicinfo.xml", "comic_info.xml", "comicinfo.json", "comic_info.json")

// ============================================================
// Utility Functions
// ============================================================

/** Removes all non-alphanumeric characters and lowercases the string. */
fun stringPress(input: String): String {
    return input.replace(Regex("[^A-Za-z0-9]+"), "").lowercase()
}

/** Applies stringPress to each element in the list. */
fun stringPressList(inputList: List<String>): List<String> {
    return inputList.map { stringPress(it) }
}

/** Removes all occurrences of items in listToRemove from the string. */
fun removeListFromString(stringToRemove: String, listToRemove: List<String>): String {
    var result = stringToRemove
    for (item in listToRemove) {
        result = result.replace(item, "")
    }
    return result.trim()
}

/** Checks if the string matches any regex pattern in the list. */
fun checkRe(stringToCheck: String, reList: List<String>): Boolean {
    val lower = stringToCheck.lowercase()
    for (pattern in reList) {
        try {
            if (Regex(pattern).matches(lower)) return true
        } catch (_: Exception) {}
    }
    return false
}

/** Returns true if string does NOT match any pattern in regex list. */
fun checkValid(stringToCheck: String, regexList: List<String>): Boolean {
    val lower = stringToCheck.lowercase()
    for (pattern in regexList) {
        try {
            if (Regex(pattern).containsMatchIn(lower)) return false
        } catch (_: Exception) {}
    }
    return true
}

/**
 * Data class holding the result of bracket extraction from a folder/file name.
 */
data class BracketResult(
    val extractedName: String,
    val circleList: List<String>,
    val curlyList: List<String>,
    val equalList: List<String>,
    val authorList: List<String>,
    val tagList: List<String>,
    val mainTitles: List<String>,
    val mainTitlesPressed: List<String>,
    val namePressed: String
)

/**
 * Extracts bracket contents from a string.
 * Port of extract_brackets() from the Python scripts.
 */
fun extractBrackets(stringToCheck: String): BracketResult {
    // Find and remove square brackets
    val findSquares = PATTERN_SQUARE_FULL.findAll(stringToCheck).toList()
    val findSquaresRemove = findSquares.map { it.groupValues[1] }
    val findSquaresList = findSquares.map { it.groupValues[2].trim() }
    val afterRemoveSquare = removeListFromString(stringToCheck, findSquaresRemove)

    // Find and remove circle brackets
    val findCircle = PATTERN_CIRCLE_FULL.findAll(afterRemoveSquare).toList()
    val findCircleRemove = findCircle.map { it.groupValues[1] }
    val findCircleList = findCircle.map { it.groupValues[2].trim() }
    val afterRemoveCircle = removeListFromString(afterRemoveSquare, findCircleRemove)

    // Find and remove curly brackets
    val findCurly = PATTERN_CURLY.findAll(afterRemoveCircle).toList()
    val findCurlyRemove = findCurly.map { it.groupValues[1] }
    val findCurlyList = findCurly.map { it.groupValues[2].trim() }
    val afterRemoveCurly = removeListFromString(afterRemoveCircle, findCurlyRemove)

    // Find and remove equal-delimited content
    var findEqualList = emptyList<String>()
    val finalString: String

    if (afterRemoveCurly.count { it == '=' } % 2 == 0) {
        val findEqual = PATTERN_EQUAL.findAll(afterRemoveCurly).toList()
        val findEqualRemove = findEqual.map { it.groupValues[1] }
        findEqualList = findEqual.map { it.groupValues[2].trim() }
        finalString = removeListFromString(afterRemoveCurly, findEqualRemove)
    } else {
        finalString = afterRemoveCurly
    }

    if (finalString.isBlank()) {
        return BracketResult(
            stringToCheck, findCircleList, findCurlyList, findEqualList,
            emptyList(), emptyList(), emptyList(), emptyList(), stringPress(stringToCheck)
        )
    }

    val mainTitles = finalString.split("_").map { it.trim() }
    val mainTitlesPressed = stringPressList(mainTitles)

    // Split to find left side (before the extracted name)
    val leftString = stringToCheck.split(finalString).firstOrNull()?.trim() ?: ""
    val leftStringSquares = PATTERN_SQUARE.findAll(leftString).map { it.groupValues[1] }.toList()

    val leftAuthorList = findSquaresList.filter { it in leftStringSquares }
    val rightTagsList = findSquaresList.filter { it !in leftStringSquares }

    return BracketResult(
        finalString, findCircleList, findCurlyList, findEqualList,
        leftAuthorList, rightTagsList, mainTitles, mainTitlesPressed,
        stringPress(stringToCheck)
    )
}

/**
 * Parses input value for search — replaces separators then extracts brackets.
 * Port of parse_input_value().
 */
fun parseInputValue(stringToCheck: String): Map<String, Any> {
    var modified = stringToCheck
    for (sep in INPUT_SEPARATORS) {
        modified = modified.replace(sep, "_")
    }
    val result = extractBrackets(modified)
    return mapOf(
        "Extracted Name" to result.extractedName,
        "Circle List" to result.circleList,
        "Curly List" to result.curlyList,
        "Equal List" to result.equalList,
        "Author List" to result.authorList,
        "Tag List" to result.tagList,
        "Main Titles" to result.mainTitles,
        "Main Titles Pressed" to result.mainTitlesPressed,
        "Name Pressed" to result.namePressed
    )
}

/**
 * Parses input value with custom separators.
 * Port of parse_input_value_custom().
 */
fun parseInputValueCustom(stringToCheck: String, customSeparators: List<String>): Map<String, Any> {
    var modified = stringToCheck
    for (sep in customSeparators) {
        modified = modified.replace(sep, "_")
    }
    val result = extractBrackets(modified)
    return mapOf(
        "Extracted Name" to result.extractedName,
        "Circle List" to result.circleList,
        "Curly List" to result.curlyList,
        "Equal List" to result.equalList,
        "Author List" to result.authorList,
        "Tag List" to result.tagList,
        "Main Titles" to result.mainTitles,
        "Main Titles Pressed" to result.mainTitlesPressed,
        "Name Pressed" to result.namePressed
    )
}

/**
 * Splits each string at the first occurrence of any separator and keeps the left part.
 * Port of split_string_and_return_1().
 */
fun splitStringAndReturn1(values: List<String>, separators: List<String>): List<String> {
    var result = values.map { it.lowercase() }
    for (sep in separators) {
        result = result.map { it.split(sep).first().trim() }
    }
    return result
}

/**
 * Converts bytes to human-readable size string.
 * Port of convert_bytes_to_readable_size().
 */
fun convertBytesToReadableSize(size: Long): String {
    val suffixes = listOf("B", "KB", "MB", "GB", "TB")
    var sizeD = size.toDouble()
    var index = 0
    while (sizeD >= 1024 && index < suffixes.size - 1) {
        sizeD /= 1024
        index++
    }
    val rounded = Math.round(sizeD * 100) / 100.0
    return "$rounded ${suffixes[index]}"
}

/** Preserves order while removing duplicates. */
fun <T> deduplicateList(items: List<T>): List<T> {
    val seen = mutableSetOf<T>()
    val deduped = mutableListOf<T>()
    for (item in items) {
        if (item !in seen) {
            seen.add(item)
            deduped.add(item)
        }
    }
    return deduped
}

/** Extracts all numbers from a filename string. */
fun extractNumbers(fileName: String): List<String> {
    return Regex("""\d+""").findAll(fileName).map { it.value }.toList()
}

/**
 * Atomically or safely replaces destFile with tempFile.
 * If destFile exists, it is replaced only when tempFile is completely written.
 */
fun safeAtomicReplace(tempFile: File, destFile: File) {
    if (!tempFile.exists()) return
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        try {
            java.nio.file.Files.move(
                tempFile.toPath(),
                destFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                java.nio.file.StandardCopyOption.ATOMIC_MOVE
            )
            return
        } catch (_: Exception) {
            try {
                java.nio.file.Files.move(
                    tempFile.toPath(),
                    destFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                )
                return
            } catch (_: Exception) {}
        }
    }
    if (destFile.exists()) {
        destFile.delete()
    }
    tempFile.renameTo(destFile)
}

