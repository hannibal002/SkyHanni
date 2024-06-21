package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

object Objective : Element() {
    override fun getDisplayPair() = buildList {
        val objective = ScoreboardPattern.objectivePattern.firstMatches(ScoreboardData.sidebarLinesFormatted) ?: return@buildList

        add(objective)
        add(ScoreboardData.sidebarLinesFormatted.nextAfter(objective) ?: HIDDEN)

        if (ScoreboardPattern.thirdObjectiveLinePattern.anyMatches(ScoreboardData.sidebarLinesFormatted)) {
            add(ScoreboardData.sidebarLinesFormatted.nextAfter(objective, 2) ?: "Second objective here")
        }
    }

    override fun showWhen() = ScoreboardPattern.objectivePattern.anyMatches(ScoreboardData.sidebarLinesFormatted)

    override val configLine = "Objective:\n§eStar SkyHanni on Github"
}
