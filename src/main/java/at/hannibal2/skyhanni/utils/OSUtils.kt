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

    fun deleteRecursively(directoryFiles: Array<File>, timeToDelete: Duration) {
        SkyHanniMod.coroutineScope.launch {
            for (file in directoryFiles) {
                val path = file.toPath()
                try {
                    val attributes = Files.readAttributes(path, BasicFileAttributes::class.java)
                    val creationTime = attributes.creationTime().toMillis()
                    val timeSinceCreation = SimpleTimeMark(creationTime).passedSince()
                    if (timeSinceCreation > timeToDelete) {
                        if (!file.deleteRecursively()) {
                            println("failed to delete directory: ${file.name}")
                        }
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                    println("Error: Unable to get creation date.")
                }
            }
        }
    }
}
