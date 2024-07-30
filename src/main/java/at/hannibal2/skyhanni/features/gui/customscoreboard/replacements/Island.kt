package at.hannibal2.skyhanni.features.gui.customscoreboard.replacements

import at.hannibal2.skyhanni.data.HypixelData

object Island : ScoreboardReplacements() {
    override val trigger: String = "%island"
    override val name: String = "Island"
    override fun replacement(): String = HypixelData.skyBlockIsland.displayName
}
