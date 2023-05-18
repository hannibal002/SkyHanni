package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import com.google.common.cache.CacheBuilder
import java.util.*
import java.util.concurrent.TimeUnit

object CopyErrorCommand {
    // random id -> error message
    private val errorMessages = mutableMapOf<String, String>()
    private var cache =
        CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build<Pair<String, Int>, Unit>()

    fun command(array: Array<String>) {
        if (array.size != 1) {
            LorenzUtils.chat("§cUse /shcopyerror <error id>")

            return
        }

        LorenzUtils.chat(errorMessages[array[0]]?.let {
            OSUtils.copyToClipboard(it)
            "§e[SkyHanni] Error copied into the clipboard, please report it on the SkyHanni discord!"
        } ?: "§c[SkyHanni] Error id not found!")
    }

    fun logError(error: Throwable, message: String) {
        val pair = error.stackTrace[0].let { it.fileName to it.lineNumber }
        if (cache.getIfPresent(pair) != null) return
        cache.put(pair, Unit)

        val errorMessage = error.toString()
        val stackTrace = error.stackTrace.asList().joinToString("\n")
        val randomId = UUID.randomUUID().toString()
        errorMessages[randomId] = "```$errorMessage\nCaused at:\n$stackTrace```"

        LorenzUtils.clickableChat(
            "§cSkyHanni ${SkyHanniMod.version} $message. Click here to copy the error into the clipboard.",
            "shcopyerror $randomId"
        )
    }
}