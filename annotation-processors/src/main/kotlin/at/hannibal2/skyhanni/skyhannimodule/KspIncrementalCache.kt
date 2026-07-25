package at.hannibal2.skyhanni.skyhannimodule

import java.io.File
import java.util.zip.CRC32

/**
 * Tracks source file states (mtime + CRC) across KSP runs to avoid unnecessary regeneration.
 *
 * @param cacheDir The directory to store the state file in, or null to disable caching.
 * @param mcVersion The Minecraft version string, used to namespace the state file.
 * @param stateFileName The base name for the state file (versioned automatically).
 */
class KspIncrementalCache(cacheDir: String?, mcVersion: String, stateFileName: String) {

    private val stateFile: File? = cacheDir?.let { File(it, "$stateFileName-$mcVersion.txt") }
    private var pendingStates: Map<String, FileState> = emptyMap()

    private data class FileState(val mtime: Long, val crc: Long)

    /**
     * Evaluates which source files are dirty and whether regeneration is needed.
     *
     * Returns null if no regeneration is needed (no dirty files and output file exists).
     * Returns a set of dirty file paths otherwise; the set may be empty if the output
     * file is missing but no sources changed. Always call [commit] after processing.
     *
     * @param filePaths The source file paths to evaluate against the cache.
     * @param outputFile The generated output file; if absent, regeneration is forced.
     */
    fun evaluate(filePaths: Set<String>, outputFile: File?): Set<String>? {
        val cachedStates = readStateFile()
        val newStates = mutableMapOf<String, FileState>()
        val dirtyPaths = mutableSetOf<String>()

        for (path in filePaths) {
            val mtime = File(path).lastModified()
            val cached = cachedStates?.get(path)
            if (cached != null && cached.mtime == mtime) {
                newStates[path] = cached
            } else {
                val crc = fileCrc(path)
                newStates[path] = FileState(mtime, crc)
                if (cached == null || cached.crc != crc) dirtyPaths.add(path)
            }
        }

        pendingStates = newStates
        if (dirtyPaths.isEmpty() && outputFile?.exists() != false) return null
        return dirtyPaths
    }

    /**
     * Returns the expected output file path relative to the build directory.
     *
     * @param packagePath The package path (slash-separated) of the generated file.
     * @param fileName The file name without extension of the generated file.
     */
    fun outputFile(packagePath: String, fileName: String): File? =
        stateFile?.parentFile?.let { File(it, "generated/ksp/main/kotlin/$packagePath/$fileName.kt") }

    /** Persists the state computed during the last [evaluate] call. */
    fun commit() = writeStateFile(pendingStates)

    private fun fileCrc(path: String): Long {
        val crc = CRC32()
        crc.update(File(path).readBytes())
        return crc.value
    }

    private fun readStateFile(): Map<String, FileState>? {
        val file = stateFile?.takeIf { it.exists() } ?: return null
        return file.readLines().mapNotNull { line ->
            val hashIdx = line.lastIndexOf('|')
            if (hashIdx < 0) return@mapNotNull null
            val mtimeIdx = line.lastIndexOf('|', hashIdx - 1)
            if (mtimeIdx < 0) return@mapNotNull null
            val path = line.substring(0, mtimeIdx)
            val mtime = line.substring(mtimeIdx + 1, hashIdx).toLongOrNull() ?: return@mapNotNull null
            val crc = line.substring(hashIdx + 1).toLongOrNull() ?: return@mapNotNull null
            path to FileState(mtime, crc)
        }.toMap()
    }

    private fun writeStateFile(states: Map<String, FileState>) {
        val file = stateFile ?: return
        file.parentFile?.mkdirs()
        file.writeText(states.entries.joinToString("\n") { (path, state) -> "$path|${state.mtime}|${state.crc}" })
    }
}
