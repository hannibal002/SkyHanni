package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.features.dungeon.DungeonApi
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.allMatches

// scoreboard
// scoreboard update event
object ScoreboardEventDungeons : ScoreboardEvent() {

    override fun getDisplay() = elementPatterns.allMatches(getSBLines()).map { it.removePrefix("§r") }

    override val configLine = "§7(All Dungeons Lines)"

    override val elementPatterns = listOf(
        ScoreboardPattern.m7dragonsPattern,
        ScoreboardPattern.autoClosingPattern,
        ScoreboardPattern.startingInPattern,
        ScoreboardPattern.keysPattern,
        ScoreboardPattern.timeElapsedPattern,
        ScoreboardPattern.clearedPattern,
        ScoreboardPattern.soloPattern,
        ScoreboardPattern.teammatesPattern,
        ScoreboardPattern.floor3GuardiansPattern,
    )

    override fun showIsland() = DungeonApi.inDungeon()
}
