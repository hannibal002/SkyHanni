package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.utils.LorenzUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.scoreboard.Score
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class ScoreboardData {

    companion object {
        var sidebarLines: List<String> = emptyList()
        var sidebarLinesRaw: List<String> = emptyList()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return

        val list = fetchScoreboardLines()
        sidebarLines = list.map { cleanSB(it) }.reversed()
        sidebarLinesRaw = list.reversed()
    }

    private fun cleanSB(scoreboard: String): String {
        return scoreboard.removeColor().toCharArray().filter { it.code in 21..126 }.joinToString(separator = "")
    }

    fun fetchScoreboardLines(): List<String> {
        val scoreboard = Minecraft.getMinecraft().theWorld?.scoreboard ?: return emptyList()
        val objective = scoreboard.getObjectiveInDisplaySlot(1) ?: return emptyList()
        var scores = scoreboard.getSortedScores(objective)
        val list = scores.filter { input: Score? ->
            input != null && input.playerName != null && !input.playerName
                .startsWith("#")
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