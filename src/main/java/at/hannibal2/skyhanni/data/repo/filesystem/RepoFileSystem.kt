package at.hannibal2.skyhanni.data.repo.filesystem

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.repo.ChatProgressUpdates
import at.hannibal2.skyhanni.data.repo.RepoLogger
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.associateNotNull
import com.google.gson.JsonElement
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private const val MAX_EMPTY_ZIP_ENTRIES = 10

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
            "Repo file '$path' is empty (0 bytes) — ${pathDiagnostics(path)}"
        }
        val content = String(bytes, Charsets.UTF_8)
        return ConfigManager.gson.fromJson(content, JsonElement::class.java)
            ?: throw IllegalStateException(
                "Repo file '$path' parsed as JSON null — file may contain only the literal 'null' or be malformed " +
                    "(${content.length} chars) — ${pathDiagnostics(path)}",
            )
    }

    /**
     * Reads [zipFile], validates each entry path, and writes each file into this [RepoFileSystem].
     *
     * Aborts and returns `false` if more than [MAX_EMPTY_ZIP_ENTRIES] entries are empty,
     * as this strongly suggests the zip is corrupt and continuing would silently produce
     * an unusable repo on disk.
     *
     * This is a plain suspend function — callers are responsible for ensuring they are already
     * running in an appropriate dispatcher (e.g. IO). No extra coroutine is launched here.
     */
    suspend fun loadFromZip(progress: ChatProgressUpdates, zipFile: File): Boolean = runCatching {
        progress.update("loadFromZip")
        ZipFile(zipFile.absolutePath).use { zip ->
            progress.update("zipFile entries collect")
            val entries = zip.entries().asSequence().filterNot { it.isDirectory }.toList()
            progress.innerProgressStart(entries.size)
            val processedPaths = entries.associateNotNull {
                val processed = it.name.substringAfter('/', it.name)
                if (processed.isEmpty()) null else processed to it
            }
            val emptySkipped = writeZipEntries(zip, processedPaths, zipFile, progress)
            progress.update("done writing zip entries ($emptySkipped empty entries skipped)")
        }
        true
    }.getOrElse { e ->
        progress.update("Failed to load repo from zip '${zipFile.name}': ${e.message}")
        logger.logNonDestructiveError("Failed to load repo from zip '${zipFile.name}': ${e.message}")
        false
    }

    /**
     * Writes each entry from [zipEntries] into this file system, validating paths and skipping empty data.
     *
     * Throws if more than [MAX_EMPTY_ZIP_ENTRIES] entries are empty, treating that as a signal
     * that [zipFile] is corrupt rather than silently producing an incomplete repo.
     *
     * @return the number of empty entries that were skipped.
     */
    private fun writeZipEntries(
        zip: ZipFile,
        zipEntries: Map<String, ZipEntry>,
        zipFile: File,
        progress: ChatProgressUpdates,
    ) = zipEntries.entries.fold(0) { emptyDataCount, (relativePath, entry) ->
        progress.innerProgressStep()
        validatePath(relativePath)
        val data = zip.getInputStream(entry).use { it.readBytes() }
        if (data.isEmpty()) {
            val incrementedCount = emptyDataCount + 1
            logger.logNonDestructiveError("Empty zip entry: $relativePath ($incrementedCount/$MAX_EMPTY_ZIP_ENTRIES)")
            check(incrementedCount <= MAX_EMPTY_ZIP_ENTRIES) {
                "Aborting: $incrementedCount empty zip entries in '${zipFile.name}' — zip is likely corrupt"
            }
            incrementedCount
        } else {
            write(relativePath, data)
            emptyDataCount
        }
    }
}
