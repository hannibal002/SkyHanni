package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.features.gui.customscoreboard.DisplayConfig
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.Companion.config
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
import at.hannibal2.skyhanni.utils.TabListData
import java.util.regex.Pattern

object CustomScoreboardUtils {
    private val numberFormat get() = config.displayConfig.numberFormat

    internal fun getGroupFromPattern(list: List<String>, pattern: Pattern, group: String) = list.map {
        it.removeResets().trimWhiteSpace()
    }.firstNotNullOfOrNull { line ->
        pattern.matchMatcher(line) {
            group(group)
        }
    } ?: "0"

    fun getProfileTypeSymbol() = when {
        HypixelData.ironman -> "§7♲ "
        HypixelData.stranded -> "§a☀ "
        HypixelData.bingo -> ScoreboardData.sidebarLinesFormatted.firstNotNullOfOrNull {
            BingoAPI.getIconFromScoreboard(it)?.plus(" ")
        } ?: "§e❤ "

        else -> "§e"
    }

    fun getTablistFooter(): String {
        val tabList = TabListData.getPlayerTabOverlay()
        if (tabList.footer_skyhanni == null) return ""
        return tabList.footer_skyhanni.formattedText.replace("§r", "")
    }

    internal fun Number.formatNum(): String = when (numberFormat) {
        DisplayConfig.NumberFormat.SHORT -> NumberUtil.format(this)
        DisplayConfig.NumberFormat.LONG -> this.addSeparators()
        else -> "0"
    }

    internal fun String.formatNum() = this.formatDouble().formatNum()

    class UndetectedScoreboardLines(message: String) : Exception(message)
}
