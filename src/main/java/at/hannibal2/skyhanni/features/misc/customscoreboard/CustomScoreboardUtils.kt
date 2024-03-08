package at.hannibal2.skyhanni.features.misc.customscoreboard

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.gui.customscoreboard.DisplayConfig
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
import at.hannibal2.skyhanni.utils.TabListData
import java.util.regex.Pattern

object CustomScoreboardUtils {
    private val config get() = SkyHanniMod.feature.gui.customScoreboard
    private val numberFormat get() = config.displayConfig.numberFormat

    internal fun getGroupFromPattern(list: List<String>, pattern: Pattern, group: String) = list.map {
        it.removeResets().trimWhiteSpace() 
        }.firstNotNullOfOrNull { line ->
             pattern.matchMatcher(line) {
                 group(group)
             }
         }  ?: "0"

    fun getProfileTypeSymbol(): String {
        return when {
            HypixelData.ironman -> "§7♲ "
            HypixelData.stranded -> "§a☀ "
            HypixelData.bingo -> ScoreboardData.sidebarLinesFormatted.firstOrNull {
                BingoAPI.getIconFromScoreboard(it) != null
            }?.let {
                BingoAPI.getIconFromScoreboard(it) + " "
            } ?: "§e❤ "

            else -> "§e"
        }
    }

    fun getTablistFooter(): String {
        val tabList = TabListData.getPlayerTabOverlay()
        if (tabList.footer_skyhanni == null) return ""
        return tabList.footer_skyhanni.formattedText.replace("§r", "")
    }

    internal fun getTitleAndFooterAlignment() = when (config.displayConfig.titleAndFooter.centerTitleAndFooter) {
        true -> HorizontalAlignment.CENTER
        false -> HorizontalAlignment.LEFT
    }

    internal fun Int.formatNum(): String = when (numberFormat) {
        DisplayConfig.NumberFormat.SHORT -> NumberUtil.format(this)
        DisplayConfig.NumberFormat.LONG -> this.addSeparators()
        else -> "0"
    }

    internal fun String.formatNum(): String {
        val number = this.formatDouble()

        return when (numberFormat) {
            DisplayConfig.NumberFormat.SHORT -> NumberUtil.format(number)
            DisplayConfig.NumberFormat.LONG -> number.addSeparators()
            else -> "0"
        }
    }

    class UndetectedScoreboardLines(message: String) : Exception(message)
}
