package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

object CopyScoreboardCommand {
    fun command(args: Array<String>) {
        val resultList = mutableListOf<String>()
        val noColor = args.size == 1 && args[0] == "true"
        resultList.add("Header:")
        resultList.add(if (noColor) ScoreboardData.objectiveLine.removeColor() else ScoreboardData.objectiveLine)
        resultList.add("")

        for (line in ScoreboardData.sidebarLinesFormatted) {
            val scoreboardLine = if (noColor) line.removeColor() else line
            resultList.add("'$scoreboardLine'")
        }

        val string = resultList.joinToString("\n")
        OSUtils.copyToClipboard(string)
        LorenzUtils.chat("§e[BedWar] scoreboard copied into your clipboard!")
    }
}