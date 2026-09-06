package com.example.lxsearch.data

import android.content.Context
import android.os.Environment
import java.io.File

/**
 * Metadata and helper operations for managing user-editable input/configuration files.
 */
object InputFileManager {

    const val APP_DIR_NAME = "LXSearch_Data"

    data class InputFileInfo(
        val fileName: String,
        val title: String,
        val shortName: String,
        val description: String,
        val syntaxHint: String,
        val usedBy: String,
        val defaultContent: String
    )

    data class FileStats(
        val lineCount: Int,
        val nonEmptyCount: Int,
        val sizeBytes: Long,
        val lastModified: Long
    )

    val DEFAULT_CHAPTER_RE = """
[\s\d-]*chapter[\s\d]*$
[\s\d-]*chapter[\s_]*tmp$
[\s\d-]*chapter[\s\d]*extra$
[\s\d-]*chapters$
read\s*online[\s_]*chapter[\s\d]*$
read\s*the\s*chapter$
[\s\d]+$
[\s\d-]*volume[\s\d]*$
oneshot
""".trimIndent()

    val DEFAULT_BRACKETS_TXT = """
color
pangean
english
digital
animated
name subject to change
en r_w
igpx
akizora momidi
manjudou
not
syamonabe
tamagoro
nylon
xter
pangean
okara
eng. ver
eng
ongoing
samples
full
decensored
the nekomancers
ayannon
mousou
impossible
son'na mon
falsche.shido
ao no roman kouro
midori
green
shiririn
niui
reupload
_
abandoned
vanilla
abbb
comfy pillow x obsoletezero
原神
九条裟羅
催眠
translated
haragami
heroine
yutaka tanaka
the people with no name
grosso
vgt13
scanmtl
colorized
original
""".trimIndent()

    val DEFAULT_BRACKETS_RE = """
^c[0-9]+
.*comic.*
^sc[0-9]+
.vol[.].
.edit by.
.translat.
.scans.
.vanilla.
.reupload.
.* tl$
.*censored.*
.*doujinshi.*
^comifuro
^ac[0-9]
^eng[.].
^.$
.[.]com.*
^ff[0-9]*
.*doujinsai.*
^spark[0-9]+
^puniket\s*[0-9]+
^colored by.*
[0-9]+[.][0-9]+
^edit by.*
^d[0-9]+
[\w]-scans$
part\s*[0-9]+
""".trimIndent()

    val DEFAULT_CHAPTER_SEP = """
ch
vol
pt
chapter
volume
part
ch.
vol.
pt.
""".trimIndent()

    val MANAGED_FILES = listOf(
        InputFileInfo(
            fileName = "exclude_folders_chapter_re.txt",
            title = "Folder & Chapter Exclusions (Regex)",
            shortName = "Chapter Regex",
            description = "Regex patterns used during file list indexing to exclude chapter/volume folders.",
            syntaxHint = "One regular expression per line (e.g. [\\s\\d-]*chapter[\\s\\d]*$)",
            usedBy = "Create File List (V1 & V2)",
            defaultContent = DEFAULT_CHAPTER_RE
        ),
        InputFileInfo(
            fileName = "exclude_in_brackets.txt",
            title = "Bracket Tag Exclusions (Exact Text)",
            shortName = "Bracket Tags",
            description = "Keywords inside brackets [like this] that will be stripped or ignored during matching.",
            syntaxHint = "One tag/keyword per line in lowercase (e.g. english, digital, original)",
            usedBy = "Move From Source, Name-Circle Relations",
            defaultContent = DEFAULT_BRACKETS_TXT
        ),
        InputFileInfo(
            fileName = "exclude_in_brackets_re.txt",
            title = "Bracket Exclusions (Regex)",
            shortName = "Bracket Regex",
            description = "Regex patterns applied to content inside brackets to strip or ignore during matching.",
            syntaxHint = "One regex pattern per line (e.g. ^c[0-9]+, .*comic.*)",
            usedBy = "Move From Source, Name-Circle Relations",
            defaultContent = DEFAULT_BRACKETS_RE
        ),
        InputFileInfo(
            fileName = "exclude_input_chapter_sep.txt",
            title = "Chapter & Volume Separators",
            shortName = "Chapter Sep",
            description = "Tokens used by the search engine to split search queries and clean file names.",
            syntaxHint = "One separator token per line (e.g. ch, vol, pt, chapter)",
            usedBy = "Search Files Engine",
            defaultContent = DEFAULT_CHAPTER_SEP
        )
    )

    /**
     * Resolves the app's root data directory in public external storage (/storage/emulated/0/LXSearch_Data).
     * Falls back to context.getExternalFilesDir(null) if external storage cannot be written.
     */
    fun getBaseDir(context: Context): File {
        val publicDir = File(Environment.getExternalStorageDirectory(), APP_DIR_NAME)
        return try {
            if (!publicDir.exists()) {
                publicDir.mkdirs()
            }
            if (publicDir.canWrite()) {
                publicDir
            } else {
                context.getExternalFilesDir(null) ?: publicDir
            }
        } catch (e: Exception) {
            context.getExternalFilesDir(null) ?: publicDir
        }
    }

    /**
     * Resolves the app's input directory (/storage/emulated/0/LXSearch_Data/input).
     */
    fun getInputDir(context: Context): File {
        val dir = File(getBaseDir(context), "input")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Resolves the app's output directory (/storage/emulated/0/LXSearch_Data/output).
     */
    fun getOutputDir(context: Context): File {
        val dir = File(getBaseDir(context), "output")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Migrates files from legacy app-specific directory (/Android/data/com.example.lxsearch/files)
     * to the new public directory (/storage/emulated/0/LXSearch_Data) if present.
     */
    fun migrateLegacyFilesIfNeeded(context: Context) {
        try {
            val legacyRoot = context.getExternalFilesDir(null) ?: return
            val targetBase = File(Environment.getExternalStorageDirectory(), APP_DIR_NAME)
            if (!targetBase.exists() && !targetBase.mkdirs()) return

            for (subDirName in listOf("input", "output")) {
                val legacySubDir = File(legacyRoot, subDirName)
                if (legacySubDir.exists() && legacySubDir.isDirectory) {
                    val targetSubDir = File(targetBase, subDirName)
                    if (!targetSubDir.exists()) targetSubDir.mkdirs()

                    legacySubDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            val dest = File(targetSubDir, file.name)
                            if (!dest.exists()) {
                                file.copyTo(dest, overwrite = false)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Finds metadata for a managed file by filename.
     */
    fun getFileInfo(fileName: String): InputFileInfo? {
        return MANAGED_FILES.find { it.fileName == fileName }
    }

    /**
     * Reads the content of an input file.
     * If the file does not exist on disk, it is copied from assets or fallback defaults.
     */
    fun readFileContent(context: Context, fileName: String): String {
        val inputDir = getInputDir(context)
        val file = File(inputDir, fileName)

        if (!file.exists()) {
            // Try loading from assets first
            val assetContent = try {
                context.assets.open("input/$fileName").bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (e: Exception) {
                null
            }

            val content = assetContent ?: getFileInfo(fileName)?.defaultContent ?: ""
            try {
                file.writeText(content, Charsets.UTF_8)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return content
        }

        return try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Saves user-edited text to the target file.
     */
    fun saveFileContent(context: Context, fileName: String, content: String): Result<Unit> {
        return runCatching {
            val inputDir = getInputDir(context)
            val file = File(inputDir, fileName)
            // Ensure trailing newline for clean parsing
            val normalized = if (content.endsWith("\n") || content.isEmpty()) content else "$content\n"
            file.writeText(normalized, Charsets.UTF_8)
        }
    }

    /**
     * Resets a file to its bundled default content.
     */
    fun resetToDefault(context: Context, fileName: String): Result<String> {
        return runCatching {
            val assetContent = try {
                context.assets.open("input/$fileName").bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (e: Exception) {
                null
            }
            val defaultText = assetContent ?: getFileInfo(fileName)?.defaultContent ?: ""
            val inputDir = getInputDir(context)
            val file = File(inputDir, fileName)
            file.writeText(defaultText, Charsets.UTF_8)
            defaultText
        }
    }

    /**
     * Calculates statistics for a given file content string.
     */
    fun computeStats(content: String, file: File? = null): FileStats {
        val lines = content.lines()
        val nonEmpty = lines.count { it.trim().isNotEmpty() }
        val sizeBytes = file?.takeIf { it.exists() }?.length() ?: content.toByteArray(Charsets.UTF_8).size.toLong()
        val lastModified = file?.takeIf { it.exists() }?.lastModified() ?: System.currentTimeMillis()
        return FileStats(
            lineCount = lines.size,
            nonEmptyCount = nonEmpty,
            sizeBytes = sizeBytes,
            lastModified = lastModified
        )
    }

    /**
     * Lists all input files, including managed files and any other .txt files in the input directory.
     */
    fun listAllInputFileNames(context: Context): List<String> {
        val inputDir = getInputDir(context)
        val managedNames = MANAGED_FILES.map { it.fileName }.toSet()
        val extraNames = (inputDir.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(".txt") && it.name !in managedNames }
            .map { it.name }
        return MANAGED_FILES.map { it.fileName } + extraNames
    }
}
