package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.features.gui.customscoreboard.DisplayConfig
import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.Companion.displayConfig
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
import at.hannibal2.skyhanni.utils.TabListData
import java.util.regex.Pattern

object CustomScoreboardUtils {

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

    internal fun Number.formatNum(): String = when (displayConfig.numberFormat) {
        DisplayConfig.NumberFormat.SHORT -> NumberUtil.format(this)
        DisplayConfig.NumberFormat.LONG -> this.addSeparators()
        else -> "0"
    }

    internal fun String.formatNum() = this.formatDouble().formatNum()

    internal fun getPurse() = PurseAPI.currentPurse.formatNum()

    internal fun getMotes() =
        getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.motesPattern, "motes").formatNum()

    internal fun getBank() =
        getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.bankPattern, "bank").formatNum()

    internal fun getBits() = BitsAPI.bits.coerceAtLeast(0).formatNum()

    internal fun getBitsAvailable() = BitsAPI.bitsAvailable.coerceAtLeast(0).formatNum()

    internal fun getBitsLine() = if (displayConfig.showUnclaimedBits) {
        "§b${getBits()}§7/§b${getBitsAvailable()}"
    } else {
        "§b${getBits()}"
    }

    internal fun getCopper() =
        getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.copperPattern, "copper").formatNum()

    internal fun getGems() =
        getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.gemsPattern, "gems").formatNum()

    internal fun getHeat() =
        getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.heatPattern, "heat").formatNum()

    internal fun getNorthStars() =
        getGroupFromPattern(
            ScoreboardData.sidebarLinesFormatted,
            ScoreboardPattern.northstarsPattern,
            "northstars"
        ).formatNum()


    class UndetectedScoreboardLines(message: String) : Exception(message)
}
