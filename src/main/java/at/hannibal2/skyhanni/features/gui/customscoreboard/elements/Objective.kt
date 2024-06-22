package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.CollectionUtils.addNotNull
import at.hannibal2.skyhanni.utils.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches

object Objective : ScoreboardElement() {
    override fun getDisplay() = buildList {
        val objective = ScoreboardPattern.objectivePattern.firstMatches(ScoreboardData.sidebarLinesFormatted) ?: return@buildList

        add(objective)
        addNotNull(ScoreboardData.sidebarLinesFormatted.nextAfter(objective))

        if (ScoreboardPattern.thirdObjectiveLinePattern.anyMatches(ScoreboardData.sidebarLinesFormatted)) {
            addNotNull(ScoreboardData.sidebarLinesFormatted.nextAfter(objective, 2))
        }
    }

    override val configLine = "Objective:\n§eStar SkyHanni on Github"
}
