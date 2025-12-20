package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import net.minecraft.Util
import java.io.File
import java.io.IOException
import java.nio.file.Files
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

    fun copyToClipboard(text: String) {
        ClipboardUtils.copyToClipboard(text)
    }

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
        SkyHanniMod.launchCoroutine("deleteExpiredFiles") {
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
}
