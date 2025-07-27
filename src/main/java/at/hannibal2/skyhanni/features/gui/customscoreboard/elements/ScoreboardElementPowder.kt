package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.api.HotmApi
import at.hannibal2.skyhanni.config.features.gui.customscoreboard.DisplayConfig.PowderDisplay
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber

// internal
// 1s internal while on mining islands?
object ScoreboardElementPowder : ScoreboardElement() {
    override fun getDisplay() = buildList {
        val powderTypes = HotmApi.PowderType.entries
        if (informationFilteringConfig.hideEmptyLines && powderTypes.all { it.total == 0L }) return@buildList

        add("§9§lPowder")

        for (type in powderTypes) {
            val name = type.displayName
            val color = type.color
            val current = formatNumber(type.current)
            val total = formatNumber(type.total)

            when (displayConfig.powderDisplay) {
                PowderDisplay.AVAILABLE -> {
                    add(" §7- ${CustomScoreboardUtils.formatNumberDisplay(name, current, color)}")
                }

                PowderDisplay.TOTAL -> {
                    add(" §7- ${CustomScoreboardUtils.formatNumberDisplay(name, total, color)}")
                }

                PowderDisplay.BOTH -> {
                    add(" §7- ${CustomScoreboardUtils.formatNumberDisplay(name, "$current/$total", color)}")
                }

                null -> {}
            }
        }
    }

    override val configLine = "§9§lPowder\n §7- §fMithril: §254,646\n §7- §fGemstone: §d51,234\n §7- §fGlacite: §b86,574"

    override fun showIsland() = IslandTypeTags.ADVANCED_MINING.inAny()
}

// click: open /hotm
