package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.data.MayorAPI
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.OSUtils

object CopyMayorData {
    fun command(args: Array<String>) {
        val string = StringBuilder()

        string.append("```yaml\n")
        string.append("Mayor Data\n")
        string.append("Last Update: ${MayorAPI.lastUpdate.toString()}\n")
        string.append("Time Till Next Mayor: ${MayorAPI.timeTillNextMayor.toString()}\n")
        string.append("Current Mayor: ${MayorAPI.currentMayor?.name ?: "Unknown"}\n")
        string.append("Active Perks: ${MayorAPI.currentMayor?.activePerks}\n")
        string.append("Candidates: ${MayorAPI.candidates.size}\n")
        string.append("```")


        OSUtils.copyToClipboard(string.toString())
        ChatUtils.chat("Mayor data copied into your clipboard!")
    }
}
