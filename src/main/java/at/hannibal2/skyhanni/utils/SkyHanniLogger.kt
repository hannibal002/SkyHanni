package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.TimeUtils.formatCurrentTime
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.io.path.pathString
import kotlin.time.Duration.Companion.days

open class SkyHanniLogger(private val filePath: String) {

    private val logFilePath: Path =
        SkyHanniMod.logsDir
            .toPath()
            .resolve(startTimeFormatted)
            .resolve("$filePath.log")

    companion object {
        private var deletedExpired = false

        private val startTimeFormatted = SimpleDateFormat("yyyy_MM_dd/HH_mm_ss").formatCurrentTime()
        private val format = SimpleDateFormat("HH:mm:ss")
    }

    @Suppress("PrintStackTrace")
    private val logger: Logger by lazy {
        Logger.getLogger("SkyHanni-Logger-" + System.nanoTime()).apply {
            try {
                logFilePath.toFile().parentFile?.takeIf { !it.isDirectory }?.mkdirs()
                FileHandler(logFilePath.pathString).apply {
                    encoding = Charsets.UTF_8.name()
                    formatter = object : Formatter() {
                        override fun format(logRecord: LogRecord) = "${format.formatCurrentTime()} ${logRecord.message}\n"
                    }
                }.let(::addHandler)
                useParentHandlers = false
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!deletedExpired && SkyBlockUtils.onHypixel) {
                deletedExpired = true
                OSUtils.deleteExpiredFiles(SkyHanniMod.logsDir, SkyHanniMod.feature.dev.logExpiryTime.days)
            }
        }
    }

    fun log(text: String?) = logger.info(text)

    /**
     * Logs [message] and the stack trace of [error] to the log file.
     * In a development environment the same text also goes to the console,
     * so a failure is visible without opening the log file first.
     */
    fun logError(message: String, error: Throwable) {
        val text = "$message\n${error.stackTraceToString()}"
        log(text)
        if (PlatformUtils.isDevEnvironment) {
            System.err.println("SkyHanni logger '$filePath': $text")
        }
    }
}
