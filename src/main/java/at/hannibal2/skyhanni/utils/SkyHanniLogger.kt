package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.TimeUtils.formatCurrentTime
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.time.Duration.Companion.days

open class SkyHanniLogger(filePath: String) {

    internal open val relativeStorage by lazy { PREFIX_PATH }
    private val format = SimpleDateFormat("HH:mm:ss")
    private val fileName by lazy { "$relativeStorage$filePath.log" }

    companion object {

        private val LOG_DIRECTORY = File("config/skyhanni/logs")
        // I'm ab to change this in another PR I CBA - daveed
        @Suppress("PropertyName")
        private var PREFIX_PATH: String
        var hasDone = false

        init {
            val format = SimpleDateFormat("yyyy_MM_dd/HH_mm_ss").formatCurrentTime()
            PREFIX_PATH = "config/skyhanni/logs/$format/"
        }
    }

    @Suppress("PrintStackTrace")
    private val logger: Logger by lazy {
        Logger.getLogger("SkyHanni-Logger-" + System.nanoTime()).apply {
            try {
                File(fileName).parentFile?.takeIf { !it.isDirectory }?.mkdirs()
                FileHandler(fileName).apply {
                    encoding = Charsets.UTF_8.name()
                    formatter = object : Formatter() {
                        override fun format(logRecord: LogRecord) = "${format.formatCurrentTime()} ${logRecord.message}\n"
                    }
                }.let(::addHandler)
                useParentHandlers = false
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (!hasDone && SkyBlockUtils.onHypixel) {
                hasDone = true
                OSUtils.deleteExpiredFiles(LOG_DIRECTORY, SkyHanniMod.feature.dev.logExpiryTime.days)
            }
        }
    }

    fun log(text: String?) = logger.info(text)
}
