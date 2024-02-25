package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.isBarn
import at.hannibal2.skyhanni.features.garden.GardenPlotAPI.name
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.client.gui.FontRenderer

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
        PurseAPI.piggyPattern.matchMatcher(text) {
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

    if (SkyHanniMod.feature.misc.colorMonthNames) {
        for (season in Season.entries) {
            if (text.trim().startsWith(season.prefix)) {
                return season.colorCode + text
            }
        }
    }

    return text
}

enum class Season(val prefix: String, val colorCode: String) {
    EARLY_SPRING("Early Spring", "§d"),
    SPRING("Spring", "§d"),
    LATE_SPRING("Late Spring", "§d"),
    EARLY_SUMMER("Early Summer", "§6"),
    SUMMER("Summer", "§6"),
    LATE_SUMMER("Late Summer", "§6"),
    EARLY_AUTUMN("Early Autumn", "§e"),
    AUTUMN("Autumn", "§e"),
    LATE_AUTUMN("Late Autumn", "§e"),
    EARLY_WINTER("Early Winter", "§9"),
    WINTER("Winter", "§9"),
    LATE_WINTER("Late Winter", "§9")
}
