package at.hannibal2.skyhanni.test.command

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.transformIf
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

object CopyScoreboardCommand {
    fun command(args: Array<String>) {
        val resultList = mutableListOf<String>()
        val noColor = args.size == 1 && args[0] == "true"
        resultList.add("Title:")
        resultList.add(ScoreboardData.objectiveTitle.transformIf({ noColor }) { removeColor() })
        resultList.add("")

        for (line in ScoreboardData.sidebarLinesFormatted) {
            val scoreboardLine = line.transformIf({ noColor }) { removeColor() }
            resultList.add("'$scoreboardLine'")
        }

        val string = resultList.joinToString("\n")
        OSUtils.copyToClipboard(string)
        LorenzUtils.chat("Scoreboard copied into your clipboard!")
    }
}
