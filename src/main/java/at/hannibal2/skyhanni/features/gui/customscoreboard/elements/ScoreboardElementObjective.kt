package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatches
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addNotNull
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets

// scoreboard
// scoreboard update event
object ScoreboardElementObjective : ScoreboardElement() {
    override fun getDisplay() = buildList {
        val objective = ScoreboardPattern.objectivePattern.firstMatches(ScoreboardData.getSidebarLinesTextCompat()) ?: return@buildList

        add(objective)
        addNotNull(ScoreboardData.getSidebarLinesTextCompat().nextAfter(objective))

        var index = 2
        while (ScoreboardPattern.thirdObjectiveLinePattern.matches(ScoreboardData.getSidebarLinesTextCompat().nextAfter(objective, index))) {
            addNotNull(listOf("", "").nextAfter(objective, index))
            index++
        }
    }

    override val configLine = "Objective:\n§eStar SkyHanni on Github"

    override val elementPatterns = listOf(
        ScoreboardPattern.objectivePattern,
        ScoreboardPattern.thirdObjectiveLinePattern,
        ScoreboardPattern.wtfAreThoseLinesPattern,
    )
}

// click: open the objective page (i think a command should exist)
