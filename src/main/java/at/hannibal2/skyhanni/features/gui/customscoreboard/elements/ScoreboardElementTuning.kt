package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.MaxwellApi
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.NumberDisplayFormat
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.utils.StringUtils.pluralize

// internal
// power update event
object ScoreboardElementTuning : ScoreboardElement() {
    private val config get() = SkyHanniMod.feature.gui.customScoreboard
    private val displayConfig get() = config.display
    private val maxwellConfig get() = config.display.maxwell

    override fun getDisplay(): Any {
        val tunings = MaxwellApi.tunings ?: return "§cTalk to \"Maxwell\"!"
        if (tunings.isEmpty()) return "§cNo Maxwell Tunings :("

        val title = pluralize(tunings.size, "Tuning")

        return if (maxwellConfig.compactTuning) {
            val tuningDisplay = tunings.take(3).joinToString("§7, ") { tuning ->
                with(tuning) {
                    when (displayConfig.numberDisplayFormat) {
                        NumberDisplayFormat.TEXT_COLOR_NUMBER -> "$icon$color$value"
                        NumberDisplayFormat.COLOR_TEXT_NUMBER -> "$color$icon$value"
                        NumberDisplayFormat.COLOR_NUMBER_TEXT -> "$color$value$icon"
                        NumberDisplayFormat.COLOR_NUMBER_RESET_TEXT -> "$color$value§f$icon"
                    }
                }
            }
            CustomScoreboardUtils.formatNumberDisplay(title, tuningDisplay, "§f")
        } else {
            val tuningAmount = maxwellConfig.tuningAmount.coerceAtLeast(1)
            val tuningList = tunings.take(tuningAmount).map { tuning ->
                with(tuning) {
                    " §7- §f" + when (displayConfig.numberDisplayFormat) {
                        NumberDisplayFormat.TEXT_COLOR_NUMBER -> "$name: $icon$color$value"
                        NumberDisplayFormat.COLOR_TEXT_NUMBER -> "$color$name: $icon$value"
                        NumberDisplayFormat.COLOR_NUMBER_TEXT -> "$color$value$icon $name"
                        NumberDisplayFormat.COLOR_NUMBER_RESET_TEXT -> "$color$value§f$icon $name"
                    }
                }
            }
            listOf("$title:") + tuningList
        }
    }

    override val configLine = "Tuning: §c❁34§7, §e⚔20§7, and §9☣7"

    override fun showIsland() = !RiftApi.inRift()
}
