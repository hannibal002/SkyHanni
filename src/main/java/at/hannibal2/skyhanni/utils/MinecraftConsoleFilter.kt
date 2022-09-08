package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Marker
import org.apache.logging.log4j.core.Filter
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.message.Message

class MinecraftConsoleFilter(private val loggerConfigName: String) : Filter {

    val lorenzLogger = LorenzLogger("debug/mc_console_log")

//    var printUnfilteredDebugs = false
//    var printUnfilteredDebugsOutsideSkyBlock = false
//    var printFilteredReason = false
//
//    var filterChat = false
//    var filterGrowBuffer = false
//    var filterUnknownSound = false
//    var filterScoreboardErrors = false

    companion object {
        @JvmStatic
        fun initLogging() {
            val ctx: LoggerContext = LogManager.getContext(false) as LoggerContext

            for (loggerConfig in ctx.configuration.loggers.values) {
                val loggerName = loggerConfig.name
                loggerConfig.addFilter(MinecraftConsoleFilter(loggerName))
            }
        }
    }

    override fun filter(event: LogEvent?): Filter.Result {
//        printUnfilteredDebugs = true
//        printUnfilteredDebugsOutsideSkyBlock = false
//        printFilteredReason = false
//
//        filterChat = true
//        filterGrowBuffer = true
//        filterUnknownSound = true
//        filterScoreboardErrors = true

        if (event == null) return Filter.Result.ACCEPT

        val loggerName = event.loggerName
        if (loggerName == "skyhanni") return Filter.Result.ACCEPT

        val message = event.message
        val formattedMessage = message.formattedMessage
        val thrown = event.thrown

        if (SkyHanniMod.feature.dev.filterChat) {
            if (formattedMessage.startsWith("[CHAT] ")) {
                filterConsole("chat")
                return Filter.Result.DENY
            }
        }
        if (SkyHanniMod.feature.dev.filterGrowBuffer) {
            if (formattedMessage.startsWith("Needed to grow BufferBuilder buffer: Old size ")) {
                filterConsole("Grow BufferBuilder buffer")
                return Filter.Result.DENY
            }
        }
        if (SkyHanniMod.feature.dev.filterUnknownSound) {
            if (formattedMessage == "Unable to play unknown soundEvent: minecraft:") {
                filterConsole("Unknown soundEvent (minecraft:)")
                return Filter.Result.DENY
            }
        }
        if (thrown != null) {
            val cause = thrown.cause
            if (cause != null) {
                if (cause.stackTrace.isNotEmpty()) {
                    val first = cause.stackTrace[0]
                    if (SkyHanniMod.feature.dev.filterScoreboardErrors) {
                        if (first.toString() == "net.minecraft.scoreboard.Scoreboard.removeTeam(Scoreboard.java:229)") {
                            filterConsole("NullPointerException at Scoreboard.removeTeam")
                            return Filter.Result.DENY
                        }
                        if (first.toString() == "net.minecraft.scoreboard.Scoreboard.createTeam(Scoreboard.java:218)") {
                            filterConsole("IllegalArgumentException at Scoreboard.createTeam")
                            return Filter.Result.DENY
                        }
                        if (first.toString() == "net.minecraft.scoreboard.Scoreboard.removeObjective(Scoreboard.java:179)") {
                            filterConsole("IllegalArgumentException at Scoreboard.removeObjective")
                            return Filter.Result.DENY
                        }
                    }
                }
            }
            if (SkyHanniMod.feature.dev.filterScoreboardErrors) {
                if (thrown.toString() == "java.util.concurrent.ExecutionException: java.lang.IllegalArgumentException: A team with the name '") {
                    filterConsole("IllegalArgumentException because scoreboard team already exists")
                    return Filter.Result.DENY
                }
            }
        }

        if (!SkyHanniMod.feature.dev.printUnfilteredDebugs) return Filter.Result.ACCEPT
        if (!SkyHanniMod.feature.dev.printUnfilteredDebugsOutsideSkyBlock && !LorenzUtils.inSkyblock) return Filter.Result.ACCEPT

        println(" ")
        println("filter 4/event ('$loggerConfigName'/'$loggerName')")
        println("formattedMessage: '$formattedMessage'")
        val threadName = event.threadName
        println("threadName: '$threadName'")
        val level = event.level
        println("level: '$level'")
        val marker = event.marker
        if (marker != null) {
            val name = marker.name
            println("marker name: '$name'")
        } else {
            println("marker is null")
        }
        println("thrown: '$thrown'")
        if (thrown != null) {
            if (thrown.stackTrace.isNotEmpty()) {
                var element = thrown.stackTrace[0]
                println("thrown first element: '$element'")
                val cause = thrown.cause
                if (cause != null) {
                    println("throw cause: '$cause'")
                    element = cause.stackTrace[0]
                    println("thrown cause first element: '$element'")
                }
            }
        }
        println(" ")

        return Filter.Result.ACCEPT
    }

    private fun filterConsole(message: String) {
        lorenzLogger.log(message)
        if (SkyHanniMod.feature.dev.printFilteredReason) {
            LorenzUtils.consoleLog("filtered console: $message")
        }
    }

    override fun getOnMismatch(): Filter.Result {
        println("getOnMismatch ($loggerConfigName)")
        return Filter.Result.DENY
    }

    override fun getOnMatch(): Filter.Result {
        println("getOnMatch ($loggerConfigName)")
        return Filter.Result.ACCEPT
    }

    override fun filter(
        logger: Logger?,
        level: Level?,
        marker: Marker?,
        msg: String?,
        vararg params: Any?,
    ): Filter.Result {
        println("filter 1 ($loggerConfigName)")
        return Filter.Result.ACCEPT
    }

    override fun filter(
        logger: Logger?,
        level: Level?,
        marker: Marker?,
        msg: Any?,
        t: Throwable?,
    ): Filter.Result {
        println("filter 2 ($loggerConfigName)")
        return Filter.Result.ACCEPT
    }

    override fun filter(
        logger: Logger?,
        level: Level?,
        marker: Marker?,
        msg: Message?,
        t: Throwable?,
    ): Filter.Result {
        println("filter 3 ($loggerConfigName)")
        return Filter.Result.ACCEPT
    }
}
