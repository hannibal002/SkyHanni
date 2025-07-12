package at.hannibal2.skyhanni.data.repo

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.Gson
import com.google.gson.JsonElement
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.ConcurrentHashMap

sealed interface RepoFileSystem {
    fun exists(path: String): Boolean
    fun readAllBytes(path: String): ByteArray
    fun write(path: String, data: ByteArray)
    fun saveToDisk(root: File)

    fun readAllBytesAsJsonElement(path: String, gson: Gson = ConfigManager.gson): JsonElement {
        val bytes = readAllBytes(path)
        val jsonText = String(bytes, Charsets.UTF_8)
        return gson.fromJson<JsonElement>(jsonText)
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
    override fun saveToDisk(root: File) {
        // No-op, already on disk
    }
}

class InMemoryRepoFileSystem : RepoFileSystem {
    private val storage = ConcurrentHashMap<String, ByteArray>()
    override fun exists(path: String) = storage.containsKey(path)
    override fun readAllBytes(path: String) = storage[path] ?: throw FileNotFoundException(path)
    override fun write(path: String, data: ByteArray) {
        storage[path] = data
    }
    override fun saveToDisk(root: File) = storage.forEach { (path, bytes) ->
        val f = File(root, path)
        f.parentFile.mkdirs()
        f.writeBytes(bytes)
    }
}
