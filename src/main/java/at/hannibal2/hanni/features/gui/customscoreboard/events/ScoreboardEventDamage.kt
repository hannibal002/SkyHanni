package at.hannibal2.hanni.features.gui.customscoreboard.events

import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.hanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.hanni.utils.RegexUtils.allMatches

// scoreboard
// scoreboard update event
object ScoreboardEventDamage : ScoreboardEvent() {

    override fun getDisplay() = elementPatterns.allMatches(getSBLines())

    override val configLine = "Dragon HP: §a6,180,925 §c❤\nYour Damage: §c375,298.5"

    override val elementPatterns = listOf(
        ScoreboardPattern.bossHPPattern,
        ScoreboardPattern.bossDamagePattern,
    )

    override fun showIsland() = IslandType.THE_END.isCurrent()
}
