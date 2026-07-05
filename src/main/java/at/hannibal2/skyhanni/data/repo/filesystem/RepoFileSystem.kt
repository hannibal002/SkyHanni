package at.hannibal2.skyhanni.data.repo.filesystem

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.repo.ChatProgressUpdates
import at.hannibal2.skyhanni.data.repo.RepoLogger
import com.google.gson.JsonElement
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File

private const val MAX_EMPTY_TGZ_ENTRIES = 10

sealed interface RepoFileSystem {
    val root: File
    val logger: RepoLogger

    fun exists(path: String): Boolean
    fun readAllBytes(path: String): ByteArray
    fun write(path: String, data: ByteArray)
    fun list(path: String): List<String>
    fun validatePath(relativePath: String) = Unit
    suspend fun transitionAfterReload(progress: ChatProgressUpdates): RepoFileSystem = this

    /**
     * Deletes everything under [path].
     * If [path] is empty, deletes all entries.
     * Should NOT delete logs.
     */
    fun deleteRecursively(path: String)

    /**
     * Returns a human-readable diagnostic string for [path] in this file system.
     * Implementations should include OS-level details where applicable (e.g. file existence,
     * size, permissions, parent directory state).
     */
    fun pathDiagnostics(path: String): String = "path='$path'"

    /**
     * Reads and parses the file at [path] as a [JsonElement].
     *
     * On failure, calls [pathDiagnostics] to surface OS-level context in the exception message,
     * so the root cause is visible without needing to inspect the stack trace.
     */
    fun readJson(path: String): JsonElement {
        val bytes = readAllBytes(path)
        check(bytes.isNotEmpty()) {
            "Repo file '$path' is empty (0 bytes)\n${pathDiagnostics(path)}"
        }
        val content = String(bytes, Charsets.UTF_8)
        return ConfigManager.gson.fromJson(content, JsonElement::class.java) ?: throw IllegalStateException(
            "Repo file '$path' parsed as JSON null. File may contain only the literal 'null' or be malformed " +
                "(${content.length} chars)\n${pathDiagnostics(path)}",
        )
    }

    /**
     * Reads [tgzFile], validates each entry path, and writes each file into this [RepoFileSystem].
     *
     * Aborts and returns `false` if more than [MAX_EMPTY_TGZ_ENTRIES] entries are empty,
     * as this strongly suggests the tar.gz file is corrupt and continuing would silently produce
     * an unusable repo on disk.
     *
     * This is a plain suspend function.
     * Callers are responsible for ensuring they are already running in an appropriate dispatcher (e.g. IO).
     */
    suspend fun loadFromTgz(progress: ChatProgressUpdates, tgzFile: File): Boolean = runCatching {
        progress.update("loadFromTgz")
        val entries = countTgzEntries(tgzFile)
        tgzFile.inputStream().use { rawInput ->
            GzipCompressorInputStream(rawInput).use { gzipInput ->
                TarArchiveInputStream(gzipInput).use { tgzInput ->
                    progress.update("tar.gz entries stream")
                    progress.innerProgressStart(entries)
                    val emptySkipped = writeTgzEntries(tgzInput, tgzFile, progress)
                    progress.update("done writing tar.gz entries ($emptySkipped empty entries skipped)")
                }
            }
        }
        true
    }.getOrElse { e ->
        progress.update("Failed to load repo from tar.gz '${tgzFile.name}': ${e.message}")
        logger.error("Failed to load repo from tar.gz '${tgzFile.name}': ${e.message}")
        false
    }

    private fun countTgzEntries(tgzFile: File): Int = tgzFile.inputStream().use { rawInput ->
        GzipCompressorInputStream(rawInput).use { gzipInput ->
            TarArchiveInputStream(gzipInput).use { tgzInput ->
                var count = 0
                var entry = tgzInput.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) count++
                    entry = tgzInput.nextEntry
                }
                count
            }
        }
    }

    /**
     * Writes each entry from [tgzInput] into this file system, validating paths and skipping empty data.
     *
     * Throws if more than [MAX_EMPTY_TGZ_ENTRIES] entries are empty, treating that as a signal
     * that [tgzFile] is corrupt rather than silently producing an incomplete repo.
     *
     * @return the number of empty entries that were skipped.
     */
    private fun writeTgzEntries(
        tgzInput: TarArchiveInputStream,
        tgzFile: File,
        progress: ChatProgressUpdates,
    ): Int {
        var emptyDataCount = 0
        var entry: ArchiveEntry? = tgzInput.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                progress.innerProgressStep()
                val relativePath = entry.name.substringAfter('/', entry.name)
                if (relativePath.isNotEmpty()) {
                    validatePath(relativePath)
                    val data = tgzInput.readBytes()
                    if (data.isEmpty()) {
                        emptyDataCount += 1
                        logger.error("Empty tar.gz entry: $relativePath ($emptyDataCount/$MAX_EMPTY_TGZ_ENTRIES)")
                        check(emptyDataCount <= MAX_EMPTY_TGZ_ENTRIES) {
                            "Aborting: $emptyDataCount empty tar.gz entries in '${tgzFile.name}'. tar.gz is likely corrupt"
                        }
                    } else {
                        write(relativePath, data)
                    }
                }
            }
            entry = tgzInput.nextEntry
        }
        return emptyDataCount
    }
}
