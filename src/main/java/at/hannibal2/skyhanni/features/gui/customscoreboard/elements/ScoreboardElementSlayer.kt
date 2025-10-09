package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.misc.ReplaceRomanNumerals

// internal
// scoreboard update event
object ScoreboardElementSlayer : ScoreboardElement() {
    override fun getDisplay() = buildList {
        if (!SlayerApi.hasActiveQuest()) return@buildList
        add("Slayer Quest")
        add(ReplaceRomanNumerals.replaceLine(SlayerApi.latestCategory))
        add(SlayerApi.latestProgress)
    }

    override fun showWhen() = if (informationFilteringConfig.hideIrrelevantLines) SlayerApi.isInCorrectArea else true

    override val configLine = "Slayer Quest\n §7- §cVoidgloom Seraph III\n §7- §e12§7/§c120 §7Kills"

    override val elementPatterns = listOf(ScoreboardPattern.slayerQuestPattern)
}
