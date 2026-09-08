package com.example.lxsearch.core

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.protocol.commons.EnumWithValue
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.ArrayDeque
import java.util.EnumSet

data class SmbFolderBasic(
    val name: String,
    val uncPath: String
)

data class SmbFolderDetails(
    val name: String,
    val uncPath: String,
    val xmlContent: String? = null,
    val itemCount: Int = 0,
    val hasFolders: Boolean = false,
    val totalSizeBytes: Long = 0L,
    val imageFilesCount: Int = 0
)

object SmbScanner {

    data class SmbComponents(
        val host: String,
        val share: String,
        val subPath: String
    )

    fun parseSmbUri(path: String): SmbComponents? {
        val clean = path.trim().replace('/', '\\')
        if (!clean.startsWith("\\\\")) return null
        val parts = clean.removePrefix("\\\\").split('\\').filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        val host = parts[0]
        val share = parts[1]
        val subPath = if (parts.size > 2) parts.drop(2).joinToString("\\") else ""
        return SmbComponents(host, share, subPath)
    }

    fun isSmbReachable(host: String, port: Int = 445, timeoutMs: Int = 2000): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun isDirectory(item: FileIdBothDirectoryInformation): Boolean {
        return EnumWithValue.EnumUtils.isSet(item.fileAttributes, FileAttributes.FILE_ATTRIBUTE_DIRECTORY)
    }

    fun scanFolders(
        locations: List<String>,
        sharePath: String,
        username: String,
        password: String,
        isCancelled: () -> Boolean = { false }
    ): List<SmbFolderBasic> {
        if (locations.isEmpty()) return emptyList()
        val firstComp = parseSmbUri(locations[0]) ?: parseSmbUri(sharePath) ?: return emptyList()
        val host = firstComp.host

        if (!isSmbReachable(host)) {
            System.err.println("[SMB] Cannot reach SMB server at $host. Skipping SMB scan.")
            return emptyList()
        }

        val result = mutableListOf<SmbFolderBasic>()
        val client = SMBClient()
        try {
            client.connect(host).use { conn ->
                val auth = AuthenticationContext(username, password.toCharArray(), "")
                val session = conn.authenticate(auth)
                session.use { sess ->
                    for (loc in locations) {
                        if (isCancelled()) break
                        val comp = parseSmbUri(loc) ?: continue
                        try {
                            val share = sess.connectShare(comp.share) as? DiskShare ?: continue
                            share.use { diskShare ->
                                if (!diskShare.folderExists(comp.subPath)) return@use
                                val queue = ArrayDeque<String>()
                                queue.add(comp.subPath)

                                while (queue.isNotEmpty()) {
                                    if (isCancelled()) break
                                    val currentPath = queue.poll() ?: break
                                    val items = try {
                                        diskShare.list(currentPath)
                                    } catch (_: Exception) {
                                        emptyList()
                                    }

                                    for (item in items) {
                                        if (item.fileName == "." || item.fileName == "..") continue
                                        if (isDirectory(item)) {
                                            val childPath = if (currentPath.isEmpty()) item.fileName else "$currentPath\\${item.fileName}"
                                            val uncPath = "\\\\$host\\${comp.share}\\$childPath"
                                            result.add(SmbFolderBasic(item.fileName, uncPath))
                                            queue.add(childPath)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            System.err.println("[SMB] Error scanning location $loc: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("[SMB] Failed SMB session connection: ${e.message}")
        }
        return result
    }

    fun scanFoldersWithDetails(
        locations: List<String>,
        sharePath: String,
        username: String,
        password: String,
        isCancelled: () -> Boolean = { false }
    ): List<SmbFolderDetails> {
        if (locations.isEmpty()) return emptyList()
        val firstComp = parseSmbUri(locations[0]) ?: parseSmbUri(sharePath) ?: return emptyList()
        val host = firstComp.host

        if (!isSmbReachable(host)) {
            System.err.println("[SMB] Cannot reach SMB server at $host. Skipping SMB scan.")
            return emptyList()
        }

        val result = mutableListOf<SmbFolderDetails>()
        val client = SMBClient()
        try {
            client.connect(host).use { conn ->
                val auth = AuthenticationContext(username, password.toCharArray(), "")
                val session = conn.authenticate(auth)
                session.use { sess ->
                    for (loc in locations) {
                        if (isCancelled()) break
                        val comp = parseSmbUri(loc) ?: continue
                        try {
                            val share = sess.connectShare(comp.share) as? DiskShare ?: continue
                            share.use { diskShare ->
                                if (!diskShare.folderExists(comp.subPath)) return@use
                                val queue = ArrayDeque<String>()
                                queue.add(comp.subPath)

                                while (queue.isNotEmpty()) {
                                    if (isCancelled()) break
                                    val currentPath = queue.poll() ?: break
                                    val items = try {
                                        diskShare.list(currentPath)
                                    } catch (_: Exception) {
                                        emptyList()
                                    }

                                    for (item in items) {
                                        if (item.fileName == "." || item.fileName == "..") continue
                                        if (isDirectory(item)) {
                                            val childPath = if (currentPath.isEmpty()) item.fileName else "$currentPath\\${item.fileName}"
                                            val uncPath = "\\\\$host\\${comp.share}\\$childPath"

                                            var itemCount = 0
                                            var hasFolders = false
                                            var totalSize = 0L
                                            var imgCount = 0
                                            var xmlText: String? = null

                                            try {
                                                val childItems = diskShare.list(childPath)
                                                val validChildren = childItems.filter { it.fileName != "." && it.fileName != ".." }
                                                itemCount = validChildren.size

                                                for (ci in validChildren) {
                                                    if (isDirectory(ci)) {
                                                        hasFolders = true
                                                    } else {
                                                        val lowerName = ci.fileName.lowercase()
                                                        if (xmlText == null && (lowerName == "comicinfo.xml" || lowerName == "comic_info.xml" || lowerName == "comicinfo.json" || lowerName == "comic_info.json")) {
                                                            try {
                                                                val smbFilePath = "$childPath\\${ci.fileName}"
                                                                diskShare.openFile(
                                                                    smbFilePath,
                                                                    EnumSet.of(AccessMask.GENERIC_READ),
                                                                    null,
                                                                    SMB2ShareAccess.ALL,
                                                                    SMB2CreateDisposition.FILE_OPEN,
                                                                    null
                                                                ).use { sf ->
                                                                    val buffer = ByteArray(65536)
                                                                    val baos = ByteArrayOutputStream()
                                                                    val inputStream = sf.inputStream
                                                                    var read: Int
                                                                    var totalRead = 0
                                                                    while (inputStream.read(buffer).also { read = it } > 0 && totalRead < 512 * 1024) {
                                                                        baos.write(buffer, 0, read)
                                                                        totalRead += read
                                                                    }
                                                                    xmlText = baos.toString("UTF-8")
                                                                }
                                                            } catch (_: Exception) {}
                                                        }
                                                        val ext = if (lowerName.contains('.')) "." + lowerName.substringAfterLast('.') else ""
                                                        if (V2_VALID_EXT.contains(ext)) {
                                                            totalSize += ci.endOfFile
                                                            imgCount++
                                                        }
                                                    }
                                                }
                                            } catch (_: Exception) {}

                                            result.add(
                                                SmbFolderDetails(
                                                    name = item.fileName,
                                                    uncPath = uncPath,
                                                    xmlContent = xmlText,
                                                    itemCount = itemCount,
                                                    hasFolders = hasFolders,
                                                    totalSizeBytes = totalSize,
                                                    imageFilesCount = imgCount
                                                )
                                            )
                                            queue.add(childPath)
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            System.err.println("[SMB] Error scanning location $loc: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("[SMB] Failed SMB session connection: ${e.message}")
        }
        return result
    }
}
