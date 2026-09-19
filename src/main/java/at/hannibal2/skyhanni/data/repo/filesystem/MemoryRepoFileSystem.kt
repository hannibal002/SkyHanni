package at.hannibal2.skyhanni.data.repo.filesystem

import at.hannibal2.skyhanni.data.repo.ChatProgressUpdates
import at.hannibal2.skyhanni.data.repo.RepoLogger
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.DisposableHandle

class MemoryRepoFileSystem(
    override val logger: RepoLogger,
) : RepoFileSystem, DisposableHandle {
    private val storage = ConcurrentHashMap<String, ByteArray>()

    override fun exists(path: String) = storage.containsKey(path)
    override fun readAllBytes(path: String) = storage[path] ?: throw FileNotFoundException(path)
    override fun write(path: String, data: ByteArray) {
        storage[path] = data
    }

    override fun pathDiagnostics(path: String) =
        "path='$path', inMemory=${storage.containsKey(path)}, totalEntries=${storage.size}"

    override fun deleteRecursively(path: String) {
        if (path.isEmpty()) {
            storage.clear()
        } else {
            val prefix = path.toPrefix()
            storage.keys.removeIf { it == path || it.startsWith(prefix) }
        }
    }

    override fun listFiles(path: String, extension: String): List<String> {
        val prefix = path.toPrefix()
        val targetSuffix = ".$extension"
        val results = mutableListOf<String>()

        for (key in storage.keys) {
            if (key.startsWith(prefix)) {
                val nextSlashIndex = key.indexOf('/', prefix.length)

                if (nextSlashIndex == -1 && key.endsWith(targetSuffix)) {
                    results.add(key.substring(prefix.length))
                }
            }
        }
        return results
    }

    override fun listDirectories(path: String): List<String> {
        val prefix = path.toPrefix()
        val results = mutableSetOf<String>()

        for (key in storage.keys) {
            if (key.startsWith(prefix)) {
                val nextSlashIndex = key.indexOf('/', prefix.length)

                if (nextSlashIndex != -1) {
                    results.add(key.substring(prefix.length, nextSlashIndex))
                }
            }
        }
        return results.toList()
    }

    override suspend fun loadFromTgz(progress: ChatProgressUpdates, tgzFile: File): Boolean {
        progress.update("repo memory file system loadFromTgz")
        val success = super.loadFromTgz(progress, tgzFile)
        progress.update("loadFromTgz end")
        return success
    }

    override fun clear() = storage.clear()

    override fun dispose() = clear()

    private fun String.toPrefix() = if (isEmpty() || endsWith("/")) this else "$this/"
}
