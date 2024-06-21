package at.hannibal2.skyhanni.features.gui.customscoreboard.events

import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getSbLines
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.features.rift.RiftAPI
import at.hannibal2.skyhanni.features.rift.area.stillgorechateau.RiftBloodEffigies
import at.hannibal2.skyhanni.utils.RegexUtils.allMatches

object Rift : Event() {
    override fun getDisplay() = listOf(
        RiftBloodEffigies.heartsPattern,
        ScoreboardPattern.riftHotdogTitlePattern,
        ScoreboardPattern.timeLeftPattern,
        ScoreboardPattern.riftHotdogEatenPattern,
        ScoreboardPattern.riftAveikxPattern,
        ScoreboardPattern.riftHayEatenPattern,
        ScoreboardPattern.cluesPattern,
    ).allMatches(getSbLines())

    override fun showWhen() = RiftAPI.inRift()

    override val configLine = "§7(All Rift Lines)"
}
