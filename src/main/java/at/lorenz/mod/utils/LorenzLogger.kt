package at.lorenz.mod.utils

import at.lorenz.mod.utils.LorenzUtils.formatCurrentTime
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.logging.FileHandler
import java.util.logging.Formatter
import java.util.logging.LogRecord
import java.util.logging.Logger

class LorenzLogger(filePath: String) {
    private val format = SimpleDateFormat("HH:mm:ss")
    private val fileName = "$PREFIX_PATH$filePath.log"

    companion object {
        private var PREFIX_PATH: String

        init {
            val format = SimpleDateFormat("yyyy_MM_dd/HH_mm_ss").formatCurrentTime()
            PREFIX_PATH = "mods/LorenzMod/logs/$format/"
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

    private fun initLogger(): Logger {
        val logger = Logger.getLogger("" + System.nanoTime())
        try {
            createParent(File(fileName))
            val handler = FileHandler(fileName)
            handler.encoding ="utf-8"
            logger.addHandler(handler)
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
        return logger
    }

    private fun createParent(file: File) {
        val parent = file.parentFile
        if (parent != null) {
            if (!parent.isDirectory) {
                parent.mkdirs()
            }
        }
    }

    fun log(text: String?) {
        getLogger().info(text)
    }
}
