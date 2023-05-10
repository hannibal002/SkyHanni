package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils

object CopyErrorCommand {
    var errorMessage = ""
    var errorStackTrace = listOf<StackTraceElement>()
    fun command() {
        try {
            if (errorMessage == "") LorenzUtils.chat("§c[SkyHanni] no error to copy") else {
                val result = errorMessage + "\nCaused at:\n" + errorStackTrace.joinToString("\n")
                OSUtils.copyToClipboard(result)
                LorenzUtils.chat("§e[SkyHanni] error message copied into the clipboard, please report it on the SkyHanni discord!")
            }
        } catch (error: Throwable) {
            OSUtils.copyToClipboard(error.toString())
            LorenzUtils.chat("§c[SkyHanni] error occurred while fetching error, please report this on the SkyHanni discord!")
        }
    }
}