package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isBarn
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.name
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.client.gui.FontRenderer

// TODO USE SH-REPO
private val piggyPattern = "Piggy: (?<coins>.*)".toPattern()

fun drawString(
    instance: FontRenderer,
    text: String,
    x: Int,
    y: Int,
    color: Int,
) = replaceString(text)?.let {
    instance.drawString(it, x, y, color)
} ?: 0

private fun replaceString(text: String, ): String? {
    if (SkyHanniMod.feature.misc.hideScoreboardNumbers && text.startsWith("§c") && text.length <= 4) {
        return null
    }
    if (SkyHanniMod.feature.misc.hidePiggyScoreboard) {
        piggyPattern.matchMatcher(text) {
            val coins = group("coins")
            return "Purse: $coins"
        }
    }

    if (SkyHanniMod.feature.garden.plotNameInScoreboard && GardenAPI.inGarden()) {
        if (text.contains("⏣")) {
            val plot = GardenPlotAPI.getCurrentPlot()
            val pests = if (text.contains("ൠ")) {
                text.last().digitToInt()
            } else 0
            var name = plot?.let {
                if (it.isBarn()) {
                    "§aGarden: The Barn"
                } else "§aPlot: §b" + it.name
            } ?: "§aGarden §cOutside"
            if (pests > 0) {
                name = "$name §7(§4${pests}ൠ§7)"
            }
            return " §7⏣ $name"
        }
    }

    return text
}
