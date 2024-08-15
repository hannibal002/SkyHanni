package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.RawScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketReceivedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.StringUtils.lastColorCode
import net.minecraft.client.Minecraft
import net.minecraft.network.play.server.S3CPacketUpdateScore
import net.minecraft.network.play.server.S3EPacketTeams
import net.minecraft.scoreboard.Score
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ScoreboardData {

    var sidebarLinesFormatted: List<String> = emptyList()

    private var sidebarLines: List<String> = emptyList() // TODO rename to raw
    var sidebarLinesRaw: List<String> = emptyList() // TODO delete
    val objectiveTitle: String get() = grabObjectiveTitle()

    fun grabObjectiveTitle(): String {
        val scoreboard = Minecraft.getMinecraft().theWorld?.scoreboard ?: return ""
        val objective = scoreboard.getObjectiveInDisplaySlot(1) ?: return ""
        return objective.displayName
    }

    private var dirty = false

    private fun formatLines(rawList: List<String>) = buildList {
        for (line in rawList) {
            val separator = splitIcons.find { line.contains(it) } ?: continue
            val split = line.split(separator)
            val start = split[0]
            var end = if (split.size > 1) split[1] else ""

            val lastColor = start.lastColorCode() ?: ""

            if (end.startsWith(lastColor)) {
                end = end.removePrefix(lastColor)
            }

            add(start + end)
        }
    }

    @HandleEvent(receiveCancelled = true)
    fun onPacketReceive(event: PacketReceivedEvent) {
        if (event.packet is S3CPacketUpdateScore) {
            if (event.packet.objectiveName == "update") {
                dirty = true
            }
        }
        if (event.packet is S3EPacketTeams) {
            if (event.packet.name.startsWith("team_")) {
                dirty = true
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onTick(event: LorenzTickEvent) {
        if (!dirty) return
        dirty = false

        val list = fetchScoreboardLines().reversed()
        val semiFormatted = list.map { cleanSB(it) }
        if (semiFormatted != sidebarLines) {
            RawScoreboardUpdateEvent(semiFormatted).postAndCatch()
            sidebarLines = semiFormatted
        }

        sidebarLinesRaw = list
        val new = formatLines(list)
        if (new != sidebarLinesFormatted) {
            ScoreboardUpdateEvent(new).postAndCatch()
            sidebarLinesFormatted = new
        }
    }

    private fun cleanSB(scoreboard: String): String {
        return scoreboard.toCharArray().filter { it.code in 21..126 || it.code == 167 }.joinToString(separator = "")
    }

    private fun fetchScoreboardLines(): List<String> {
        val scoreboard = Minecraft.getMinecraft().theWorld?.scoreboard ?: return emptyList()
        val objective = scoreboard.getObjectiveInDisplaySlot(1) ?: return emptyList()
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
}
