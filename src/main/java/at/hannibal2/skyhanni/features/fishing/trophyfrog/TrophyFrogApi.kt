package at.hannibal2.skyhanni.features.fishing.trophyfrog

import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.StringUtils.splitLines
import at.hannibal2.skyhanni.utils.compat.defaultStyleConstructor
import at.hannibal2.skyhanni.utils.compat.setHoverShowText
import net.minecraft.network.chat.Style

object TrophyFrogApi {

    fun hoverInfo(rawName: String): String? {
        val counts = TrophyFrogManager.frog?.get(rawName) ?: return null
        val coloredName = TrophyFrogManager.getDisplayName(rawName)
        val bestObtained = counts.filter { it.value > 0 }.keys.maxOrNull() ?: TrophyRarity.BRONZE

        val lines = mutableListOf(coloredName)
        TrophyFrogManager.frogDescriptions?.get(rawName)?.let { lines.add("§7${it.splitLines(150)}") }
        lines.add("")
        lines.add("${TrophyRarity.DIAMOND.formattedString}: ${formatCount(counts, TrophyRarity.DIAMOND)}")
        lines.add("${TrophyRarity.GOLD.formattedString}: ${formatCount(counts, TrophyRarity.GOLD)}")
        lines.add("${TrophyRarity.SILVER.formattedString}: ${formatCount(counts, TrophyRarity.SILVER)}")
        lines.add("${TrophyRarity.BRONZE.formattedString}: ${formatCount(counts, TrophyRarity.BRONZE)}")
        lines.add("")
        lines.add("§7Total: ${bestObtained.formatCode}${counts.values.sum().addSeparators()}")
        return lines.joinToString("\n")
    }

    fun getTooltip(rawName: String): Style? {
        val display = hoverInfo(rawName) ?: return null
        return defaultStyleConstructor.setHoverShowText(display)
    }

    private fun formatCount(counts: Map<TrophyRarity, Int>, rarity: TrophyRarity): String {
        val count = counts.getOrDefault(rarity, 0)
        return if (count > 0) "§6${count.addSeparators()}" else "§c✖"
    }
}
