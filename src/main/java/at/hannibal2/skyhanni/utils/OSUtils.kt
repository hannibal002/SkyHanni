package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import net.minecraft.util.Util
import java.io.File
import java.io.IOException
import java.nio.file.DirectoryIteratorException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.time.Duration

object OSUtils {
    enum class OperatingSystem {
        LINUX,
        SOLARIS,
        WINDOWS,
        MACOS,
        UNKNOWN,
    }

    fun getOperatingSystemRaw(): String = System.getProperty("os.name")

    fun getOperatingSystem(): OperatingSystem {
        val osName = getOperatingSystemRaw().lowercase()
        return when {
            osName.contains("win") -> OperatingSystem.WINDOWS
            osName.contains("mac") -> OperatingSystem.MACOS
            osName.contains("solaris") || osName.contains("sunos") -> OperatingSystem.SOLARIS
            osName.contains("linux") || osName.contains("unix") -> OperatingSystem.LINUX

            else -> OperatingSystem.UNKNOWN
        }
    }

    val isWindows: Boolean
    val isMac: Boolean
    val isLinux: Boolean
    val isSolaris: Boolean

    init {
        val os = getOperatingSystem()
        isWindows = os == OperatingSystem.WINDOWS
        isMac = os == OperatingSystem.MACOS
        isLinux = os == OperatingSystem.LINUX
        isSolaris = os == OperatingSystem.SOLARIS
    }

    @JvmStatic
    fun openBrowser(url: String) {
        Util.getPlatform().openUri(url)
    }

    @JvmStatic
    @Suppress("MaxLineLength")
    fun openSoundsListInBrowser() {
        val url = "https://misode.github.io/sounds/"

        openBrowser(url)
    }

    @Deprecated(
        "Use copyToClipboardAsync instead for a success boolean return",
        ReplaceWith("copyToClipboardAsync(text)"),
    )
    fun copyToClipboard(text: String) = ClipboardUtils.copyToClipboard(text)

    suspend fun copyToClipboardAsync(text: String): Boolean? = ClipboardUtils.copyToClipboardAsync(text).await()

    fun readFromClipboard() = ClipboardUtils.readFromClipboard()

    private fun File.isExpired(
        expiryDuration: Duration,
        lastModifiedTime: SimpleTimeMark = lastModifiedTime(),
    ): Boolean = lastModifiedTime.passedSince() > expiryDuration

    private fun File.lastModifiedTime(): SimpleTimeMark = try {
        val attributes = Files.readAttributes(toPath(), BasicFileAttributes::class.java)
        SimpleTimeMark(attributes.lastModifiedTime().toMillis())
    } catch (e: IOException) {
        ErrorManager.logErrorWithData(
            e,
            "Error reading last modified attributes",
            "file" to this,
            "path" to this.absolutePath,
        )
        SimpleTimeMark.now()
    }

    private fun File.isEmptyFile() = length() == 0L
    private fun File.isEmptyDirectory() = listFiles()?.isEmpty() == true

    /**
     * Recursively deletes files and directories inside the root directory.
     *
     * Empty or expired files are deleted. Files are considered expired if their last modified time
     * exceeds the specified expiry duration.
     * Directories are removed if they are empty after file deletion.
     * Files modified on the three most recent distinct dates are always retained.
     *
     * @param root the starting directory for recursive deletion.
     * @param expiryDuration the duration threshold used to determine if a file is expired.
     */
    fun deleteExpiredFiles(root: File, expiryDuration: Duration) {
        CoroutineSettings("deleteExpiredFiles").launch {
            val allFiles = root.walk().filter { it.isFile }.toList()
            val lastModified = allFiles.associateWith { file ->
                file.lastModifiedTime()
            }

            @Suppress("ConvertCallChainIntoSequence")
            val recentDays = lastModified.mapNotNull { it.value.toLocalDate() }
                .distinct()
                .sortedDescending()
                .take(3)
                .toSet()

            root.walkBottomUp().forEach { file ->
                when {
                    file.isFile -> {
                        val lastModifiedTime = lastModified[file] ?: file.lastModifiedTime()
                        if (lastModifiedTime.toLocalDate() in recentDays) return@forEach

                        if (file.isEmptyFile() || file.isExpired(expiryDuration, lastModifiedTime)) {
                            file.deleteWithError()
                        }
                    }

                    file.isDirectory && file.isEmptyDirectory() -> {
                        file.deleteWithError()
                    }
                }
            }
        }
    }

    fun File.deleteWithError() {
        if (!this.delete()) {
            ErrorManager.logErrorStateWithData(
                "Failed to delete file",
                "Failed to delete file",
                "file" to this,
                "path" to this.absolutePath,
            )
        }
    }

    /**
     * True for a symbolic link and, on Windows, a directory junction, also when the target is gone.
     * [Files.isSymbolicLink] alone misses junctions.
     */
    fun Path.isLinkOrJunction(): Boolean {
        if (Files.isSymbolicLink(this)) return true
        if (!isWindows) return false
        val attributes = runCatching {
            Files.readAttributes(this, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
        }.getOrNull() ?: return false
        // Junctions report isOther on every Java version, cloud folders do too on Java 25.
        if (!attributes.isOther) return false
        val parent = toAbsolutePath().parent ?: return false
        // Only a junction resolves to somewhere else. Anything unresolvable counts as a link, the safe side.
        return runCatching {
            toRealPath() != parent.toRealPath().resolve(toRealPath(LinkOption.NOFOLLOW_LINKS).fileName)
        }.getOrDefault(true)
    }

    /**
     * Deletes this file or directory recursively. A link or junction is deleted itself, never
     * the files it points at, see [isLinkOrJunction]. The first failure is reported through
     * [ErrorManager], together with the number of failures.
     *
     * @return true if the deletion succeeded, also when nothing existed.
     */
    fun File.deleteRecursivelySafe(): Boolean {
        val failures = mutableListOf<IOException>()
        toPath().deleteWithoutFollowingLinks(failures)
        val firstFailure = failures.firstOrNull() ?: return true
        ErrorManager.logErrorWithData(
            firstFailure,
            "Failed to delete files",
            "path" to absolutePath,
            "failures" to failures.size,
        )
        return false
    }

    // Not Path.deleteRecursively, it enters junctions on Java 25 (JDK-8364277).
    private fun Path.deleteWithoutFollowingLinks(failures: MutableList<IOException>) {
        try {
            val attributes = Files.readAttributes(this, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
            if (attributes.isDirectory && !isLinkOrJunction()) {
                val failuresBefore = failures.size
                Files.newDirectoryStream(this).use { entries ->
                    for (entry in entries) entry.deleteWithoutFollowingLinks(failures)
                }
                // Children left over, deleting this directory would only fail too.
                if (failures.size > failuresBefore) return
            }
            Files.delete(this)
        } catch (_: java.nio.file.NoSuchFileException) {
            // Already gone. Qualified, the unqualified name is the unrelated kotlin.io.NoSuchFileException.
        } catch (e: DirectoryIteratorException) {
            failures.add(e.cause ?: IOException(e))
        } catch (e: IOException) {
            failures.add(e)
        }
    }
}
