package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.KeyboardUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.common.cache.CacheBuilder
import net.minecraft.client.Minecraft
import java.util.UUID
import java.util.concurrent.TimeUnit

object CopyErrorCommand {
    // random id -> error message
    private val errorMessages = mutableMapOf<String, String>()
    private val fullErrorMessages = mutableMapOf<String, String>()
    private var cache =
        CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build<Pair<String, Int>, Unit>()

    fun skyHanniError(message: String): Nothing {
        val exception = IllegalStateException(message)
        logError(exception, message)
        throw exception
    }

    fun command(array: Array<String>) {
        if (array.size != 1) {
            LorenzUtils.chat("§cUse /shcopyerror <error id>")
            return
        }

        val id = array[0]
        val fullErrorMessage = KeyboardUtils.isControlKeyDown()
        val errorMessage = if (fullErrorMessage) {
            fullErrorMessages[id]
        } else {
            errorMessages[id]
        }
        val name = if (fullErrorMessage) "Ful error" else "Error"
        LorenzUtils.chat(errorMessage?.let {
            OSUtils.copyToClipboard(it)
            "§e[SkyHanni] $name copied into the clipboard, please report it on the SkyHanni discord!"
        } ?: "§c[SkyHanni] Error id not found!")
    }

    fun logErrorState(userMessage: String, internalMessage: String) {
        logError(IllegalStateException(internalMessage), userMessage)
    }

    fun logError(throwable: Throwable, message: String) {
        val error = Error(message, throwable)
        Minecraft.getMinecraft().thePlayer ?: throw error
        error.printStackTrace()

        val pair = if (throwable.stackTrace.isNotEmpty()) {
            throwable.stackTrace[0].let { it.fileName to it.lineNumber }
        } else message to 0
        if (cache.getIfPresent(pair) != null) return
        cache.put(pair, Unit)

        val fullStackTrace = throwable.getExactStackTrace(true).joinToString("\n")
        val stackTrace = throwable.getExactStackTrace(false).joinToString("\n").removeSpam()
        val randomId = UUID.randomUUID().toString()

        val rawMessage = message.removeColor()
        errorMessages[randomId] = "```\nSkyHanni ${SkyHanniMod.version}: $rawMessage\n \n$stackTrace\n```"
        fullErrorMessages[randomId] =
            "```\nSkyHanni ${SkyHanniMod.version}: $rawMessage\n(full stack trace)\n \n$fullStackTrace\n```"

        LorenzUtils.clickableChat(
            "§c[SkyHanni ${SkyHanniMod.version}]: $message§c. Click here to copy the error into the clipboard.",
            "shcopyerror $randomId"
        )
    }
}

private fun Throwable.getExactStackTrace(full: Boolean, parent: List<String> = emptyList()): List<String> = buildList {
    add("Caused by " + javaClass.name + ": $message")

    val breakAfter = listOf(
        "at net.minecraftforge.client.ClientCommandHandler.executeCommand(",
    )
    val replace = mapOf(
        "io.mouberry,notenoughupdates" to "NEU",
        "at.hannibal2.skyhanni" to "SH",
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

    cause?.let {
        addAll(it.getExactStackTrace(full, this))
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
        "CopyErrorCommand.logErrorState(CopyErrorCommand.kt:46)",
        "LorenzEvent.postWithoutCatch(LorenzEvent.kt:24)",
        "LorenzEvent.postAndCatch(LorenzEvent.kt:15)",
        "at net.minecraft.launchwrapper.",
        "at net.fabricmc.devlaunchinjector.",
    )
    return split("\n").filter { line -> !ignored.any { line.contains(it) } }.joinToString("\n")
}
