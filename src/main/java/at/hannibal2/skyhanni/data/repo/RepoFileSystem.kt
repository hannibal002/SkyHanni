package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.Gson
import com.google.gson.JsonElement
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import kotlin.time.Duration.Companion.minutes

sealed interface RepoFileSystem {
    fun exists(path: String): Boolean
    fun readAllBytes(path: String): ByteArray
    fun write(path: String, data: ByteArray)
    fun list(path: String): List<String>
    suspend fun transitionAfterReload(progress: ChatProgressUpdates): RepoFileSystem = this

    /**
     * Deletes everything under [path].
     * If [path] is empty, deletes all entries.
     */
    fun deleteRecursively(path: String)
    fun deleteAll() = deleteRecursively("")

    fun readAllBytesAsJsonElement(path: String, gson: Gson = ConfigManager.gson): JsonElement {
        val bytes = readAllBytes(path)
        val jsonText = String(bytes, Charsets.UTF_8)
        return gson.fromJson<JsonElement>(jsonText)
    }

    fun loadFromZip(
        progress: ChatProgressUpdates,
        zipFile: File,
        logger: RepoLogger,
    ): Boolean = runCatching {
        progress.update("loadFromZip")
        ZipFile(zipFile.absolutePath).use { zip ->
            progress.update("zipFile entries collect")
            val entries = zip.entries().asSequence()
                .filterNot { it.isDirectory }
                .toList()
            progress.innerProgressStart(entries.size)
            for (entry in entries) {
                progress.innerProgressStep()
                val relative = entry.name.substringAfter('/', entry.name)
                if (relative.isBlank()) continue

                if (this@RepoFileSystem is DiskRepoFileSystem) {
                    val outPath = root.toPath().resolve(relative).normalize()
                    if (!outPath.startsWith(root.toPath())) {
                        throw RuntimeException(
                            "SkyHanni detected an invalid zip file. This is a potential security risk, " +
                                "please report this on the SkyHanni discord.",
                        )
                    }
                }

                zip.getInputStream(entry).use { input ->
                    val data = input.readBytes()
                    write(relative, data)
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

    companion object {
        fun createAndClean(root: File, useMemory: Boolean): RepoFileSystem {
            val fs = if (useMemory) MemoryRepoFileSystem(root) else DiskRepoFileSystem(root)
            fs.deleteAll()
            return fs
        }
    }
}

class DiskRepoFileSystem(val root: File) : RepoFileSystem {
    override fun exists(path: String) = File(root, path).isFile
    override fun readAllBytes(path: String) = File(root, path).readBytes()
    override fun write(path: String, data: ByteArray) {
        val f = File(root, path)
        f.parentFile.mkdirs()
        f.writeBytes(data)
    }

    override fun deleteRecursively(path: String) {
        File(root, path).deleteRecursively()
    }

    override fun list(path: String) = root.resolve(path).listFiles { file ->
        file.exists() && file.extension == "json"
    }?.mapNotNull { it.name }?.toList().orEmpty()
}

class MemoryRepoFileSystem(private val diskRoot: File) : RepoFileSystem, DisposableHandle {
    private val storage = ConcurrentHashMap<String, ByteArray>()
    private var flushJob: Job? = null

    override fun exists(path: String) = storage.containsKey(path)
    override fun readAllBytes(path: String) = storage[path] ?: throw FileNotFoundException(path)
    override fun write(path: String, data: ByteArray) {
        storage[path] = data
    }

    override fun deleteRecursively(path: String) {
        if (path.isEmpty()) storage.clear()
        else storage.keys.removeIf { it == path || it.startsWith("$path/") }
    }

    override fun list(path: String) = storage.keys.filter {
        it.startsWith("$path/") && it.removePrefix("$path/").endsWith(".json")
    }.map { it.removePrefix("$path/") }

    override fun loadFromZip(progress: ChatProgressUpdates, zipFile: File, logger: RepoLogger): Boolean {
        progress.update("repo file system loadFromZip")
        val success = super.loadFromZip(progress, zipFile, logger)
        if (flushJob == null) {
            progress.update("start new launchIOCoroutine task")
            flushJob = SkyHanniMod.launchIOCoroutine("repo file saveToDisk", timeout = 2.minutes) {
                saveToDisk(progress.category, diskRoot)
            }
        }
        progress.update("loadFromZip end")
        return success
    }

    override fun dispose() = storage.clear()

    override suspend fun transitionAfterReload(progress: ChatProgressUpdates): RepoFileSystem {
        progress.update("waiting on flushJob")
        runBlocking { flushJob?.join() }
        progress.update("dispose")
        dispose()
        progress.update("transitionAfterReload end")
        return DiskRepoFileSystem(diskRoot)
    }

    private fun saveToDisk(group: ChatProgressUpdates.ChatProgressCategory, root: File) {
        val progress = group.start("saveToDisk")

        val base = root.toPath()
        progress.update("createDirectoriesFor")
        base.createDirectoriesFor(storage.keys)
        progress.update("parallelStream forEach resolve write")
        val entries = storage.entries.toList()
        progress.innerProgressStart(entries.size)
        entries.parallelStream().forEach { (relativePath, bytes) ->
            progress.innerProgressStep()
            val out = base.resolve(relativePath)
            Files.write(out, bytes)
        }

        progress.end("saveToDisk end")
    }

    private fun Path.createDirectoriesFor(relativePaths: Set<String>) = relativePaths.mapNotNull { p ->
        Paths.get(p).parent
    }.toSet().forEach { dir ->
        Files.createDirectories(this.resolve(dir))
    }
}
