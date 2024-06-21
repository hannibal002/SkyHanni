package at.hannibal2.skyhanni.features.gui.customscoreboard.elements

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.MaxwellAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.maxwellConfig
import at.hannibal2.skyhanni.utils.LorenzUtils.inAnyIsland
import at.hannibal2.skyhanni.utils.StringUtils.pluralize

object Tuning : Element() {
    override fun getDisplayPair(): List<Any> {
        val tunings = MaxwellAPI.tunings ?: return listOf("§cTalk to \"Maxwell\"!")
        if (tunings.isEmpty()) return listOf("§cNo Maxwell Tunings :(")

        val title = pluralize(tunings.size, "Tuning", "Tunings")
        return if (maxwellConfig.compactTuning) {
            val tuning = tunings
                .take(3)
                .joinToString("§7, ") { tuning ->
                    with(tuning) {
                        if (displayConfig.displayNumbersFirst) {
                            "$color$value$icon"
                        } else "$color$icon$value"
                    }

                }
            listOf(
                (if (displayConfig.displayNumbersFirst) {
                    "$tuning §f$title"
                } else "$title: $tuning"),
            )
        } else {
            val tuning = tunings
                .take(maxwellConfig.tuningAmount.coerceAtLeast(1))
                .map { tuning ->
                    with(tuning) {
                        " §7- §f" + if (displayConfig.displayNumbersFirst) {
                            "$color$value $icon $name"
                        } else "$name: $color$value$icon"
                    }
                }.toTypedArray()
            listOf("$title:", *tuning)
        }
    }

    override fun showWhen() = !inAnyIsland(IslandType.THE_RIFT)

    override val configLine = "Tuning: §c❁34§7, §e⚔20§7, and §9☣7"
}
