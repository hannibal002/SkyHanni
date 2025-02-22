package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import kotlinx.coroutines.launch
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
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

    init {
        val os = getOperatingSystem()
        isWindows = os == OperatingSystem.WINDOWS
        isMac = os == OperatingSystem.MACOS
        isLinux = os == OperatingSystem.LINUX
    }

    @JvmStatic
    fun openBrowser(url: String) {
        val desktopSupported = Desktop.isDesktopSupported()
        val supportedActionBrowse = Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
        if (desktopSupported && supportedActionBrowse) {
            try {
                Desktop.getDesktop().browse(URI(url))
            } catch (e: IOException) {
                ErrorManager.logErrorWithData(
                    e,
                    "Error while opening website.",
                    "url" to url,
                )
            }
        } else {
            copyToClipboard(url)
            ErrorManager.logErrorStateWithData(
                "Cannot open website! Copied url to clipboard instead", "Web browser is not supported",
                "url" to url,
                "desktopSupported" to desktopSupported,
                "supportedActionBrowse" to supportedActionBrowse,
            )
        }
    }

    fun copyToClipboard(text: String) {
        ClipboardUtils.copyToClipboard(text)
    }

    suspend fun readFromClipboard() = ClipboardUtils.readFromClipboard()

    private fun File.isExpired(expiryDuration: Duration): Boolean = lastModifiedTime()?.let {
        it.passedSince() > expiryDuration
    } ?: false

    private fun File.lastModifiedTime(): SimpleTimeMark? = try {
        val attributes = Files.readAttributes(toPath(), BasicFileAttributes::class.java)
        SimpleTimeMark(attributes.lastModifiedTime().toMillis())
    } catch (e: IOException) {
        SimpleTimeMark.now()
    }

    private fun File.isEmptyFile() = length() == 0L
    private fun File.isEmptyDirectory() = listFiles()?.isEmpty() == true

    /**
     * Recursively deletes files and directories inside the root directory.
     *
     * Empty or expired files are deleted.
     * They are deemed expired if their last modified time is longer than the given expiry duration
     * Directories are deleted if they are empty after deleting all files inside
     *
     * @param root the starting directory for recursive deletion.
     * @param expiryDuration the duration threshold to check if a file is expired.
     */
    fun deleteExpiredFiles(root: File, expiryDuration: Duration) {
        SkyHanniMod.coroutineScope.launch {
            root.walkBottomUp().forEach { file ->
                when {
                    file.isFile && (file.isEmptyFile() || file.isExpired(expiryDuration)) -> {
                        if (!file.delete()) {
                            println("Failed to delete file: ${file.absolutePath}")
                        }
                    }

                    file.isDirectory && file.isEmptyDirectory() -> {
                        if (!file.delete()) {
                            println("Failed to delete empty directory: ${file.absolutePath}")
                        }
                    }
                }
            }
        }
    }
}
