package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.ScoreboardChangeEvent
import at.hannibal2.skyhanni.events.ScoreboardRawChangeEvent
import net.minecraft.client.Minecraft
import net.minecraft.scoreboard.Score
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ScoreboardData {

    companion object {
        // TODO USE SH-REPO
        private val splitIcons = listOf(
            "\uD83C\uDF6B",
            "\uD83D\uDCA3",
            "\uD83D\uDC7D",
            "\uD83D\uDD2E",
            "\uD83D\uDC0D",
            "\uD83D\uDC7E",
            "\uD83C\uDF20",
            "\uD83C\uDF6D",
            "⚽",
            "\uD83C\uDFC0",
            "\uD83D\uDC79",
            "\uD83C\uDF81",
            "\uD83C\uDF89",
            "\uD83C\uDF82",
            "\uD83D\uDD2B",
        )

        fun formatLines(rawList: List<String>): List<String> {
            val list = mutableListOf<String>()
            for (line in rawList) {
                val separator = splitIcons.find { line.contains(it) } ?: continue
                val split = line.split(separator)
                val start = split[0]
                var end = split[1]
                // get last color code in start
                val lastColorIndex = start.lastIndexOf('§')
                val lastColor = when {
                    lastColorIndex != -1 && lastColorIndex + 1 < start.length -> start[lastColorIndex] + "" + start[lastColorIndex + 1]
                    else -> ""
                }

                // remove first color code from end, when it is the same as the last color code in start
                if (end.length >= 2 && lastColor.isNotEmpty() && end[0] == '§' && (end[1] in '0'..'9' || end[1] in 'a'..'f') && end[1] == lastColor[1]) {
                    end = end.substring(2)
                }
                list.add(start + end)
            }

            return list
        }

        var sidebarLinesFormatted: List<String> = emptyList()

        var sidebarLines: List<String> = emptyList() // TODO rename to raw
        var sidebarLinesRaw: List<String> = emptyList() // TODO delete
        var objectiveTitle = ""
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onTick(event: LorenzTickEvent) {

        val list = fetchScoreboardLines().reversed()
        val semiFormatted = list.map { cleanSB(it) }
        if (semiFormatted != sidebarLines) {
            ScoreboardRawChangeEvent(sidebarLines, semiFormatted).postAndCatch()
            sidebarLines = semiFormatted
        }

        sidebarLinesRaw = list
        val new = formatLines(list)
        if (new != sidebarLinesFormatted) {
            ScoreboardChangeEvent(sidebarLinesFormatted, new).postAndCatch()
            sidebarLinesFormatted = new
        }
    }

    private fun cleanSB(scoreboard: String): String {
        return scoreboard.toCharArray().filter { it.code in 21..126 || it.code == 167 }.joinToString(separator = "")
    }

    private fun fetchScoreboardLines(): List<String> {
        val scoreboard = Minecraft.getMinecraft().theWorld?.scoreboard ?: return emptyList()
        val objective = scoreboard.getObjectiveInDisplaySlot(1) ?: return emptyList()
        objectiveTitle = objective.displayName
        var scores = scoreboard.getSortedScores(objective)
        val list = scores.filter { input: Score? ->
            input != null && input.playerName != null && !input.playerName.startsWith("#")
        }
        scores = if (list.size > 15) {
            list.drop(15)
        } else {
            list
        }
        return scores.map {
            ScorePlayerTeam.formatPlayerName(scoreboard.getPlayersTeam(it.playerName), it.playerName)
        }
    }
}
