package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.common.cache.CacheBuilder
import net.minecraft.client.Minecraft
import java.util.UUID
import java.util.concurrent.TimeUnit

object ErrorManager {
    // random id -> error message
    private val errorMessages = mutableMapOf<String, String>()
    private val fullErrorMessages = mutableMapOf<String, String>()
    private var cache =
        CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build<Pair<String, Int>, Unit>()

    fun resetCache() {
        cache.asMap().clear()
    }

    fun skyHanniError(message: String): Nothing {
        val exception = IllegalStateException(message)
        logError(exception, message)
        throw exception
    }

    fun command(array: Array<String>) {
        if (array.size != 1) {
            LorenzUtils.userError("Use /shcopyerror <error id>")
            return
        }

        val id = array[0]
        val fullErrorMessage = KeyboardManager.isControlKeyDown()
        val errorMessage = if (fullErrorMessage) {
            fullErrorMessages[id]
        } else {
            errorMessages[id]
        }
        val name = if (fullErrorMessage) "Full error" else "Error"
        LorenzUtils.chat(errorMessage?.let {
            OSUtils.copyToClipboard(it)
            "$name copied into the clipboard, please report it on the SkyHanni discord!"
        } ?: "Error id not found!")
    }

    @Deprecated("Use data as well", ReplaceWith("logErrorStateWithData()"))
    fun logErrorState(userMessage: String, internalMessage: String) {
        logError(IllegalStateException(internalMessage), userMessage, false)
    }

    fun logErrorStateWithData(userMessage: String, internalMessage: String, vararg extraData: Pair<String, Any?>) {
        logError(IllegalStateException(internalMessage), userMessage, false, *extraData)
    }

    fun logError(throwable: Throwable, message: String, vararg extraData: Pair<String, Any?>) {
        logError(throwable, message, false, *extraData)
    }

    fun logError(
        throwable: Throwable,
        message: String,
        ignoreErrorCache: Boolean,
        vararg extraData: Pair<String, Any?>
    ) {
        val error = Error(message, throwable)
        error.printStackTrace()
        Minecraft.getMinecraft().thePlayer ?: return

        if (!ignoreErrorCache) {
            val pair = if (throwable.stackTrace.isNotEmpty()) {
                throwable.stackTrace[0].let { it.fileName to it.lineNumber }
            } else message to 0
            if (cache.getIfPresent(pair) != null) return
            cache.put(pair, Unit)
        }

        val fullStackTrace = throwable.getCustomStackTrace(true).joinToString("\n")
        val stackTrace = throwable.getCustomStackTrace(false).joinToString("\n").removeSpam()
        val randomId = UUID.randomUUID().toString()

        val extraDataString = buildExtraDataString(extraData)
        val rawMessage = message.removeColor()
        errorMessages[randomId] =
            "```\nSkyHanni ${SkyHanniMod.version}: $rawMessage\n \n$stackTrace\n$extraDataString```"
        fullErrorMessages[randomId] =
            "```\nSkyHanni ${SkyHanniMod.version}: $rawMessage\n(full stack trace)\n \n$fullStackTrace\n$extraDataString```"

        LorenzUtils.clickableChat(
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
}

private fun Throwable.getCustomStackTrace(full: Boolean, parent: List<String> = emptyList()): List<String> = buildList {
    add("Caused by " + javaClass.name + ": $message")

    val breakAfter = listOf(
        "at net.minecraftforge.client.ClientCommandHandler.executeCommand(",
    )
    val replace = mapOf(
        "io.mouberry,notenoughupdates" to "NEU",
        "at.hannibal2.skyhanni" to "SH",
        "net.minecraft." to "MC.",
        "net.minecraftforge.fml." to "FML.",
    )

    for (traceElement in stackTrace) {
        var text = "\tat $traceElement"
        if (!full && text in parent) {
            println("broke at: $text")
            break
        }
        if (!full) {
            for ((from, to) in replace) {
                text = text.replace(from, to)
            }
        }
        add(text)
        if (!full && breakAfter.any { text.contains(it) }) {
            println("breakAfter: $text")
            break
        }
    }

    if (this === cause) {
        add("Infinite recurring causes")
        return@buildList
    }

    cause?.let {
        addAll(it.getCustomStackTrace(full, this))
    }
}

private fun String.removeSpam(): String {
    val ignored = listOf(
        "at io.netty.",
        "at net.minecraft.network.",
        "at net.minecraftforge.fml.common.network.handshake.",
        "at java.lang.Thread.run",
        "at com.google.gson.internal.",
        "at net.minecraftforge.fml.common.eventhandler.",
        "at java.util.concurrent.",
        "at sun.reflect.",
        "at net.minecraft.client.Minecraft.addScheduledTask(",
        "at java.lang.reflect.",
        "at at.hannibal2.skyhanni.config.commands.Commands\$",
        ".ErrorManager.logErrorState(ErrorManager.kt:51)",
        "LorenzEvent.postWithoutCatch(LorenzEvent.kt:24)",
        "LorenzEvent.postAndCatch(LorenzEvent.kt:15)",
        "at net.minecraft.launchwrapper.",
        "at net.fabricmc.devlaunchinjector.",
        "at SH.events.LorenzEvent.postAndCatchAndBlock(LorenzEvent.kt:28)",
        "at SH.events.LorenzEvent.postAndCatchAndBlock\$default(LorenzEvent.kt:18)",
        "at SH.events.LorenzEvent.postAndCatch(LorenzEvent.kt:16)",
    )
    return split("\n").filter { line -> !ignored.any { line.contains(it) } }.joinToString("\n")
}
