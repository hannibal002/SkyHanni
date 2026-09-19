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
            val prefix = if (path.endsWith("/")) path else "$path/"
            storage.keys.removeIf { it == path || it.startsWith(prefix) }
        }
    }

    override fun list(path: String): List<String> {
        val prefix = when {
            path.isEmpty() -> ""
            path.endsWith("/") -> path
            else -> "$path/"
        }

        return storage.keys.mapNotNull { key ->
            if (key.startsWith(prefix)) {
                val relativePath = key.removePrefix(prefix)
                if (!relativePath.contains("/") && relativePath.endsWith(".json")) {
                    relativePath
                } else null
            } else null
        }
    }

    /**
     * Loads entries from [tgzFile] into in-memory storage (via [loadFromTgz])
     */
    override suspend fun loadFromTgz(progress: ChatProgressUpdates, tgzFile: File): Boolean {
        progress.update("repo memory file system loadFromTgz")
        val success = super.loadFromTgz(progress, tgzFile)
        progress.update("loadFromTgz end")
        return success
    }

    override fun clear() = storage.clear()

    override fun dispose() = clear()
}
