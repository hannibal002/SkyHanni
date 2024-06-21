package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.SlayerAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.HIDDEN

object Slayer : Element() {
    override fun getDisplayPair() = buildList {
        add((if (SlayerAPI.hasActiveSlayerQuest()) "Slayer Quest" else HIDDEN))
        add(" §7- §e${SlayerAPI.latestSlayerCategory.trim()}")
        add(" §7- §e${SlayerAPI.latestSlayerProgress.trim()}")
    }

    override fun showWhen() = if (informationFilteringConfig.hideIrrelevantLines) SlayerAPI.isInCorrectArea else true

    override val configLine = "Slayer Quest\n §7- §cVoidgloom Seraph III\n §7- §e12§7/§c120 §7Kills"
}
