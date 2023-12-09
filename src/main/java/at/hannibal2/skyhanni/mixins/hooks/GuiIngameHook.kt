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

private fun replaceString(text: String): String? {
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
            val hasPests = text.contains("ൠ")
            val pestSuffix = if (hasPests) {
                val pests = text.last().digitToInt()
                val color = if (pests >= 4) "§c" else "§6"
                " §7(${color}${pests}ൠ§7)"
            } else ""
            val name = plot?.let {
                if (it.isBarn()) "§aThe Barn" else {
                    val namePrefix = if (hasPests) "" else "§aPlot §7- "
                    "$namePrefix§b" + it.name
                }
            } ?: "§aGarden §coutside"
            return " §7⏣ $name$pestSuffix"
        }
    }

    return text
}
