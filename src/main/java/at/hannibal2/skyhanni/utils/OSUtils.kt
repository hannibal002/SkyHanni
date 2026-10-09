package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.time.Duration
import kotlinx.coroutines.Job

//? if >= 26.3 {
import com.mojang.blaze3d.Blaze3D
import java.net.URI
//?} else {
/*import net.minecraft.util.Util
*///?}

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
            osName.contains("win") -> WINDOWS
            osName.contains("mac") -> MACOS
            osName.contains("solaris") || osName.contains("sunos") -> SOLARIS
            osName.contains("linux") || osName.contains("unix") -> LINUX
            else -> UNKNOWN
        }
    }

    val isWindows: Boolean
    val isMac: Boolean
    val isLinux: Boolean
    val isSolaris: Boolean

    init {
        val os = getOperatingSystem()
        isWindows = os == WINDOWS
        isMac = os == MACOS
        isLinux = os == LINUX
        isSolaris = os == SOLARIS
    }

    @JvmStatic
    fun openBrowser(url: String) {
        //? if >= 26.3 {
        Blaze3D.openUri(URI(url))
        //?} else
        //Util.getPlatform().openUri(url)
    }

    @JvmStatic
    fun openSoundsListInBrowser() {
        openBrowser("https://misode.github.io/sounds/")
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

    private val backgroundDeleteCoroutine = CoroutineSettings("deleteExpiredFiles")

    /**
     * Recursively deletes files and directories inside the root directory.
     *
     * Empty or expired files are deleted. Files are considered expired if their last modified time
     * exceeds the specified expiry duration.
     * Directories are removed if they are empty after file deletion.
     * Files modified on the three most recent distinct dates are always retained.
     *
     * @param root The starting directory for recursive deletion.
     * @param expiryDuration The duration threshold used to determine if a file is expired.
     */
    fun deleteExpiredFiles(root: File, expiryDuration: Duration): Job = backgroundDeleteCoroutine.launch {
        val allFiles = root.walk().filter { it.isFile }.toList()
        val lastModified = allFiles.associateWith { file ->
            file.lastModifiedTime()
        }

        val recentDays = lastModified
            .asSequence()
            .mapNotNull { it.value.toLocalDate() }
            .distinct()
            .sortedDescending()
            .take(3)
            .toSet()

        root.walkBottomUp().forEach { file ->
            if (file.isFile) {
                val lastModifiedTime = lastModified[file] ?: file.lastModifiedTime()
                if (lastModifiedTime.toLocalDate() in recentDays) return@forEach

                if (file.isEmptyFile() || file.isExpired(expiryDuration, lastModifiedTime)) {
                    file.deleteWithError()
                }
            } else if (file.isDirectory && file.isEmptyDirectory()) {
                file.deleteWithError()
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
