package at.hannibal2.skyhanni.data.repo.filesystem

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.repo.ChatProgressUpdates
import at.hannibal2.skyhanni.data.repo.RepoLogger
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.associateNotNull
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.JsonElement
import java.io.File
import java.util.zip.ZipFile

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

    fun readJson(path: String): JsonElement = readAllBytes(path).let {
        val byteString = String(it, Charsets.UTF_8)
        ConfigManager.gson.fromJson<JsonElement>(byteString)
    }

    /**
     * Reads [zipFile], validates each entry path, and writes each file into this [RepoFileSystem].
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
            processedPaths.forEach { (relativePath, entry) ->
                progress.innerProgressStep()
                validatePath(relativePath)
                zip.getInputStream(entry).use { input ->
                    val data = input.readBytes()
                    write(relativePath, data)
                }
            }
            progress.update("done with forEach")
        }
        true
    }.getOrElse {
        progress.update("Failed to load repo from zip file: ${zipFile.absolutePath}")
        logger.logNonDestructiveError("Failed to load repo from zip file: ${zipFile.absolutePath}")
        false
    }
}

