package at.hannibal2.skyhanni.data.repo.filesystem

import at.hannibal2.skyhanni.SkyHanniMod.launchUnScoped
import at.hannibal2.skyhanni.data.repo.ChatProgressUpdates
import at.hannibal2.skyhanni.data.repo.RepoLogger
import at.hannibal2.skyhanni.utils.coroutines.CoroutineConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

class MemoryRepoFileSystem(
    override val root: File,
    override val logger: RepoLogger,
    private val coroutineConfig: CoroutineConfig,
) : RepoFileSystem, DisposableHandle {
    private val storage = ConcurrentHashMap<String, ByteArray>()

    /**
     * Tracks the result of the background disk-flush started in [loadFromZip].
     * Completed (successfully or exceptionally) before [transitionAfterReload] proceeds.
     */
    private var flushResult: CompletableDeferred<Unit>? = null

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

    /**
     * Loads entries from [zipFile] into in-memory storage (via [loadFromZip]), then
     * kicks off a background [flushResult] job to persist those bytes to [root].
     *
     * The flush is intentionally deferred to after the reload event fires, so that event
     * handlers benefit from fast in-memory reads without waiting for disk I/O. Call
     * [transitionAfterReload] to wait for the flush and switch to [DiskRepoFileSystem].
     */
    override suspend fun loadFromZip(progress: ChatProgressUpdates, zipFile: File): Boolean {
        progress.update("repo file system loadFromZip")
        val success = super.loadFromZip(progress, zipFile)
        check(flushResult == null) { "loadFromZip called twice on the same MemoryRepoFileSystem instance" }

        // Snapshot the category reference now — storage may be cleared before the flush job reads it.
        val progressCategory = progress.category
        val deferred = CompletableDeferred<Unit>()
        flushResult = deferred

        // Launched into the module-level scope so it outlives this call site and can be
        // awaited later in transitionAfterReload.
        // We use CompletableDeferred to propagate success or failure, because launchUnScoped routes
        // through runWithErrorHandling which would otherwise swallow exceptions silently.
        coroutineConfig.withIOContext().launchUnScoped {
            try {
                saveToDisk(progressCategory, root)
                deferred.complete(Unit)
            } catch (e: CancellationException) {
                deferred.completeExceptionally(e)
                // Have to re-throw to ensure propagation
                throw e
            } catch (e: Throwable) {
                deferred.completeExceptionally(e)
            }
        }

        progress.update("loadFromZip end")
        return success
    }

    override fun dispose() = storage.clear()

    /**
     * Waits for the background disk flush to complete, then disposes in-memory storage and
     * returns a [DiskRepoFileSystem] backed by [root].
     *
     * If the flush failed, the error is logged and the transition still proceeds — callers
     * should treat unsuccessful repo constants as the signal that something went wrong on disk.
     */
    override suspend fun transitionAfterReload(progress: ChatProgressUpdates): RepoFileSystem {
        val deferred = flushResult
        flushResult = null

        deferred?.let {
            progress.update("waiting for disk flush")
            runCatching { it.await() }.onFailure { e ->
                // Disk state may be incomplete. We still transition so that memory is freed,
                // but callers will observe failures via unsuccessfulConstants.
                progress.update("disk flush failed — repo on disk may be incomplete: ${e.message}")
            }
        }

        progress.update("dispose in-memory storage")
        dispose()
        return DiskRepoFileSystem(root, logger)
    }

    /**
     * Persists all in-memory entries to [root] in parallel using structured concurrency.
     *
     * Using [kotlinx.coroutines.coroutineScope] + [launch] here (rather than any module-level launch helper)
     * ensures that:
     *  - every child write is a structural child of this coroutine
     *  - a failure in any single write cancels siblings and propagates to the caller
     *  - cancellation of the parent automatically cancels all writes
     */
    private suspend fun saveToDisk(
        group: ChatProgressUpdates.ChatProgressCategory,
        root: File,
    ) = group.startSuspendBlock("saveToDisk") { progress ->
        val base = root.toPath()
        progress.update("createDirectoriesFor")
        base.createDirectoriesFor(storage.keys)

        val entries = storage.entries.toList()
        progress.innerProgressStart(entries.size)
        progress.update("writing entries")

        coroutineScope {
            for ((relativePath, bytes) in entries) {
                launch {
                    Files.write(base.resolve(relativePath), bytes)
                    progress.innerProgressStep()
                }
            }
        }

        progress.end("saveToDisk end")
    }

    private fun Path.createDirectoriesFor(relativePaths: Set<String>) = relativePaths.mapNotNull { p ->
        Paths.get(p).parent
    }.toSet().forEach { dir ->
        Files.createDirectories(this.resolve(dir))
    }
}
