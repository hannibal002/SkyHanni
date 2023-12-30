package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.gui.customscoreboard.DisplayConfig
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.AlignmentEnum
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import net.minecraft.client.Minecraft
import java.util.regex.Pattern

object CustomScoreboardUtils {
    private val config get() = SkyHanniMod.feature.gui.customScoreboard
    val numberFormat get() = config.displayConfig.numberFormat

    fun getGroupFromPattern(list: List<String>, pattern: Pattern, group: String): String {
        val matchedLine = list.map { it.removeResets().trimWhiteSpaceAndResets().removeResets() }
            .firstNotNullOfOrNull { line ->
                pattern.matchMatcher(line) {
                    group(group)
                }
            }

        return matchedLine ?: "0"
    }

    fun getProfileTypeSymbol(): String {
        return when {
            HypixelData.ironman -> "§7♲ " // Ironman
            HypixelData.stranded -> "§a☀ " // Stranded
            HypixelData.bingo -> ScoreboardData.sidebarLines.firstOrNull { it.contains("Bingo") }?.substring(
                0,
                3
            ) + "Ⓑ " // Bingo - gets the first 3 chars of " §9Ⓑ §9Bingo" (you are unable to get the Ⓑ for some reason)
            else -> "§e" // Default case
        }
    }

    fun getTablistFooter(): String {
        val tabList = Minecraft.getMinecraft().ingameGUI.tabList as AccessorGuiPlayerTabOverlay
        if (tabList.footer_skyhanni == null) return ""
        return tabList.footer_skyhanni.formattedText.replace("§r", "")
    }

    fun getTitleAndFooterAlignment() = when (config.displayConfig.titleAndFooter.centerTitleAndFooter) {
        true -> AlignmentEnum.CENTER
        false -> AlignmentEnum.LEFT
    }

    fun Int.formatNum(): String = when (numberFormat) {
        DisplayConfig.NumberFormat.SHORT -> NumberUtil.format(this)
        DisplayConfig.NumberFormat.LONG -> this.addSeparators()
        else -> "0"
    }

    fun String.formatNum(): String {
        val number = this.replace(",", "").toIntOrNull() ?: return "0"

        return when (numberFormat) {
            DisplayConfig.NumberFormat.SHORT -> NumberUtil.format(number)
            DisplayConfig.NumberFormat.LONG -> number.addSeparators()
            else -> "0"
        }
    }

    class UndetectedScoreboardLines(message: String) : Exception(message)
}
