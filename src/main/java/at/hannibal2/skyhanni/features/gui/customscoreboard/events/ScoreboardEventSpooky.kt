package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSBLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.chat.TextHelper

// scoreboard && tablist
// scoreboard update event and tablist footer update event
object ScoreboardEventSpooky : ScoreboardEvent() {
    override fun getDisplay() = buildList {
        ScoreboardPattern.spookyPattern.firstMatches(getSBLines())?.let { time ->
            add(time)
            add("§7Your Candy: ")
            TabListData.footer?.let { footerComponent ->
                val lines = TextHelper.split(footerComponent, "\n") ?: listOf(footerComponent)
                val matchLine = lines.firstOrNull { it.string.startsWith("Your Candy:") }?.string?.removePrefix("§7Your Candy: ")
                add(matchLine ?: "§cCandy not found")
            }

        }
    }

    // TODO: Add isSpookyActive() somewhere

    override val configLine = "§6Spooky Festival§f 50:54\n§7Your Candy:\n§a1 Green§7, §50 Purple §7(§61 §7pts.)"

    override val elementPatterns = listOf(ScoreboardPattern.spookyPattern)
}
