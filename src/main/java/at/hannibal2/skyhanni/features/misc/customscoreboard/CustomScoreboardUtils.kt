package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.mixins.transformers.AccessorGuiPlayerTabOverlay
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.AlignmentEnum
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.client.Minecraft
import java.util.regex.Pattern

object CustomScoreboardUtils {
    private val config get() = SkyHanniMod.feature.gui.customScoreboard

    fun getGroupFromPattern(pattern: Pattern, group: String): String {
        return ScoreboardData.sidebarLinesFormatted.firstNotNullOfOrNull { line ->
            pattern.matchMatcher(line) {
                group(group)
            }
        } ?: "0"
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
        return LorenzUtils.stripVanillaMessage(tabList.footer_skyhanni.formattedText)
    }

    fun getTitleAndFooterAlignment() = when (config.displayConfig.titleAndFooter.centerTitleAndFooter) {
        true -> AlignmentEnum.CENTER
        false -> AlignmentEnum.LEFT
    }

    class UndetectedScoreboardLines(message: String) : Exception(message)
}
