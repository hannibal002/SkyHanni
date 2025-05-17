package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
//#if TODO
import at.hannibal2.skyhanni.utils.LorenzUtils.formatCurrentTime
//#endif
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger
import kotlin.time.Duration.Companion.days

// todo 1.21 impl needed
class LorenzLogger(filePath: String) {

    private val format = SimpleDateFormat("HH:mm:ss")
    private val fileName = "$PREFIX_PATH$filePath.log"

    companion object {

        private val LOG_DIRECTORY = File("config/skyhanni/logs")
        private var PREFIX_PATH: String
        var hasDone = false
        // todo remove once lorenz utils works
        //#if MC > 1.21
        //$$ fun SimpleDateFormat.formatCurrentTime(): String = this.format(System.currentTimeMillis())
        //#endif

        init {
            val format = SimpleDateFormat("yyyy_MM_dd/HH_mm_ss").formatCurrentTime()
            PREFIX_PATH = "config/skyhanni/logs/$format/"
        }
    }

    private lateinit var logger: Logger

    private fun getLogger(): Logger {
        if (::logger.isInitialized) {
            return logger
        }

        val initLogger = initLogger()
        this.logger = initLogger
        return initLogger
    }

    @Suppress("PrintStackTrace")
    private fun initLogger(): Logger {
        val logger = Logger.getLogger("Lorenz-Logger-" + System.nanoTime())
        try {
            createParent(File(fileName))
            val handler = FileHandler(fileName)
            handler.encoding = "utf-8"
            logger.addHandler(handler)
            logger.useParentHandlers = false
            handler.formatter = object : Formatter() {
                override fun format(logRecord: LogRecord): String {
                    val message = logRecord.message
                    return format.formatCurrentTime() + " $message\n"
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        //#if TODO
        if (!hasDone && LorenzUtils.onHypixel) {
            //#else
            //$$  if (!hasDone ) {
            //#endif
            hasDone = true
            OSUtils.deleteExpiredFiles(LOG_DIRECTORY, SkyHanniMod.feature.dev.logExpiryTime.days)
        }

        return logger
    }

    private fun createParent(file: File) {
        val parent = file.parentFile
        if (parent != null && !parent.isDirectory) {
            parent.mkdirs()
        }
    }

    fun log(text: String?) {
        getLogger().info(text)
    }
}
