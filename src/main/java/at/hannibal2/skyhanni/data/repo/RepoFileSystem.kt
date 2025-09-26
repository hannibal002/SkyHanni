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
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile
import kotlin.sequences.forEach
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
        ZipFile(zipFile.absolutePath).use { zip ->
            zip.entries().asSequence().filter { !it.isDirectory }.forEach { entry ->
                val relative = entry.name.substringAfter('/', "").takeIf { it.isNotBlank() }
                    ?: return@forEach

                if (this@RepoFileSystem is DiskRepoFileSystem) {
                    // Security: ensure the file is within the root directory
                    val outPath = root.toPath().resolve(relative).normalize()
                    if (!outPath.startsWith(root.toPath())) throw RuntimeException(
                        "SkyHanni detected an invalid zip file. This is a potential security risk, " +
                            "please report this on the SkyHanni discord.",
                    )
                }

                val data = zip.getInputStream(entry).readBytes()
                write(relative, data)
            }
        }
        true
    }.getOrElse {
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
        progress.update("loadFromZip start")
        val success = super.loadFromZip(progress, zipFile, logger)
        if (flushJob == null) {
            progress.update("start new launchIOCoroutine task")
            flushJob = SkyHanniMod.launchIOCoroutine("repo file saveToDisk", timeout = 2.minutes) {
                val asyncProgress = ChatProgressUpdates()
                asyncProgress.start("repo file saveToDisk")
                saveToDisk(asyncProgress, diskRoot)
                asyncProgress.end("done saving file")
            }
        }
        progress.update("loadFromZip end")
        return success
    }

    override fun dispose() = storage.clear()

    override suspend fun transitionAfterReload(progress: ChatProgressUpdates): RepoFileSystem {
        progress.update("transitionAfterReload start")
        runBlocking { flushJob?.join() }
        dispose()
        progress.update("transitionAfterReload end")
        return DiskRepoFileSystem(diskRoot)
    }

    private fun saveToDisk(progress: ChatProgressUpdates, root: File) {
        progress.update("saveToDisk start")
        val base = root.toPath()
        progress.update("createDirectoriesFor")
        base.createDirectoriesFor(storage.keys)
        progress.update("parallelStream forEach resolve write")
        val stream = storage.entries.parallelStream()
        val done = AtomicInteger(0)
        progress.innerProgress(0, stream.count().toInt())
        stream.forEach { (relativePath, bytes) ->
            val out = base.resolve(relativePath)
            Files.write(out, bytes)
            progress.innerProgress(done.incrementAndGet(), stream.count().toInt())
        }
        progress.update("saveToDisk end")
    }

    private fun Path.createDirectoriesFor(relativePaths: Set<String>) = relativePaths.mapNotNull { p ->
        Paths.get(p).parent
    }.toSet().forEach { dir ->
        Files.createDirectories(this.resolve(dir))
    }
}
