package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeLimitedSet
import net.minecraft.client.Minecraft
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

object ErrorManager {

    // random id -> error message
    private val errorMessages = mutableMapOf<String, String>()
    private val fullErrorMessages = mutableMapOf<String, String>()
    private var cache = TimeLimitedSet<Pair<String, Int>>(10.minutes)

    private val breakAfter = listOf(
        "at at.hannibal2.skyhanni.config.commands.Commands\$createCommand",
        "at net.minecraftforge.fml.common.eventhandler.EventBus.post",
    )

    private val replace = mapOf(
        "at.hannibal2.skyhanni" to "SH",
        "io.mouberry,notenoughupdates" to "NEU",
        "net.minecraft." to "MC.",
        "net.minecraftforge.fml." to "FML.",
    )

    private val ignored = listOf(
        "at java.lang.Thread.run",
        "at java.util.concurrent.",
        "at java.lang.reflect.",
        "at net.minecraft.network.",
        "at net.minecraft.client.Minecraft.addScheduledTask(",
        "at net.minecraftforge.fml.common.network.handshake.",
        "at net.minecraftforge.fml.common.eventhandler.",
        "at net.fabricmc.devlaunchinjector.",
        "at io.netty.",
        "at com.google.gson.internal.",
        "at sun.reflect.",

        "at at.hannibal2.skyhanni.config.commands.SimpleCommand.",
        "at at.hannibal2.skyhanni.config.commands.Commands\$createCommand\$1.processCommand",
        "at at.hannibal2.skyhanni.test.command.ErrorManager.logError",
        "at at.hannibal2.skyhanni.events.LorenzEvent.postAndCatchAndBlock",
        "at net.minecraft.launchwrapper.",
    )

    fun resetCache() {
        cache.clear()
    }

    fun skyHanniError(message: String): Nothing {
        val exception = IllegalStateException(message)
        logErrorWithData(exception, message)
        throw exception
    }

    fun command(array: Array<String>) {
        if (array.size != 1) {
            ChatUtils.userError("Use /shcopyerror <error id>")
            return
        }

        val id = array[0]
        val fullErrorMessage = KeyboardManager.isModifierKeyDown()
        val errorMessage = if (fullErrorMessage) {
            fullErrorMessages[id]
        } else {
            errorMessages[id]
        }
        val name = if (fullErrorMessage) "Full error" else "Error"
        ChatUtils.chat(errorMessage?.let {
            OSUtils.copyToClipboard(it)
            "$name copied into the clipboard, please report it on the SkyHanni discord!"
        } ?: "Error id not found!")
    }

    @Deprecated("Use data as well", ReplaceWith("ErrorManager.logErrorStateWithData(userMessage, internalMessage)"))
    fun logErrorState(userMessage: String, internalMessage: String) {
        logError(IllegalStateException(internalMessage), userMessage, ignoreErrorCache = false, noStackTrace = false)
    }

    fun logErrorStateWithData(
        userMessage: String,
        internalMessage: String,
        vararg extraData: Pair<String, Any?>,
        ignoreErrorCache: Boolean = false,
        noStackTrace: Boolean = false,
    ) {
        logError(IllegalStateException(internalMessage), userMessage, ignoreErrorCache, noStackTrace, *extraData)
    }

    @Deprecated("Use data as well", ReplaceWith("ErrorManager.logErrorWithData(throwable, message)"))
    fun logError(throwable: Throwable, message: String) {
        logError(throwable, message, ignoreErrorCache = false, noStackTrace = false)
    }

    fun logErrorWithData(
        throwable: Throwable,
        message: String,
        vararg extraData: Pair<String, Any?>,
        ignoreErrorCache: Boolean = false,
    ) {
        logError(throwable, message, ignoreErrorCache, noStackTrace = false, *extraData)
    }

    private fun logError(
        throwable: Throwable,
        message: String,
        ignoreErrorCache: Boolean,
        noStackTrace: Boolean,
        vararg extraData: Pair<String, Any?>,
    ) {
        val error = Error(message, throwable)
        error.printStackTrace()
        Minecraft.getMinecraft().thePlayer ?: return

        if (!ignoreErrorCache) {
            val pair = if (throwable.stackTrace.isNotEmpty()) {
                throwable.stackTrace[0].let { it.fileName!! to it.lineNumber }
            } else message to 0
            if (cache.contains(pair)) return
            cache.add(pair)
        }

        val fullStackTrace: String
        val stackTrace: String

        if (noStackTrace) {
            fullStackTrace = "<no stack trace>"
            stackTrace = "<no stack trace>"
        } else {
            fullStackTrace = throwable.getCustomStackTrace(true).joinToString("\n")
            stackTrace = throwable.getCustomStackTrace(false).joinToString("\n")
        }
        val randomId = UUID.randomUUID().toString()

        val extraDataString = buildExtraDataString(extraData)
        val rawMessage = message.removeColor()
        errorMessages[randomId] =
            "```\nSkyHanni ${SkyHanniMod.version}: $rawMessage\n \n$stackTrace\n$extraDataString```"
        fullErrorMessages[randomId] =
            "```\nSkyHanni ${SkyHanniMod.version}: $rawMessage\n(full stack trace)\n \n$fullStackTrace\n$extraDataString```"

        ChatUtils.clickableChat(
            "§c[SkyHanni-${SkyHanniMod.version}]: $message§c. Click here to copy the error into the clipboard.",
            "shcopyerror $randomId",
            false
        )
    }

    private fun buildExtraDataString(extraData: Array<out Pair<String, Any?>>): String {
        val extraDataString = if (extraData.isNotEmpty()) {
            val builder = StringBuilder()
            for ((key, value) in extraData) {
                builder.append(key)
                builder.append(": ")
                if (value is Iterable<*>) {
                    builder.append("\n")
                    for (line in value) {
                        builder.append(" - '$line'")
                        builder.append("\n")
                    }
                } else {
                    builder.append("'$value'")
                }
                builder.append("\n")
            }
            "\nExtra data:\n$builder"
        } else ""
        return extraDataString
    }

    private fun Throwable.getCustomStackTrace(
        fullStackTrace: Boolean,
        parent: List<String> = emptyList()
    ): List<String> = buildList {
        add("Caused by ${this@getCustomStackTrace.javaClass.name}: $message")

        for (traceElement in stackTrace) {
            val text = "\tat $traceElement"
            if (!fullStackTrace && text in parent) {
                println("broke at: $text")
                break
            }
            var visualText = text
            if (!fullStackTrace) {
                for ((from, to) in replace) {
                    visualText = visualText.replace(from, to)
                }
            }
            if (!fullStackTrace && breakAfter.any { text.contains(it) }) {
                break
            }
            if (ignored.any { text.contains(it) }) continue
            add(visualText)
        }

        if (this === cause) {
            add("<Infinite recurring causes>")
            return@buildList
        }

        cause?.let {
            addAll(it.getCustomStackTrace(fullStackTrace, this))
        }
    }
}
