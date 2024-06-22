package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.features.gui.customscoreboard.DisplayConfig
import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
import at.hannibal2.skyhanni.utils.TabListData
import java.util.regex.Pattern

object CustomScoreboardUtils {

    internal fun getGroup(pattern: Pattern, list: List<String>, group: String) =
        list.map { it.removeResets().trimWhiteSpace() }.firstNotNullOfOrNull { line ->
            pattern.matchGroup(line, group)
        }

    fun getProfileTypeSymbol() = when {
        HypixelData.ironman -> "§7♲ "
        HypixelData.stranded -> "§a☀ "
        HypixelData.bingo -> ScoreboardData.sidebarLinesFormatted.firstNotNullOfOrNull {
            BingoAPI.getIconFromScoreboard(it)?.plus(" ")
        } ?: "§e❤ "

        else -> "§e"
    }

    internal fun formatNumber(number: Number): String = when (displayConfig.numberFormat) {
        DisplayConfig.NumberFormat.SHORT -> number.shortFormat()
        DisplayConfig.NumberFormat.LONG -> number.addSeparators()
        else -> "0"
    }

    internal fun formatStringNum(string: String) = formatNumber(string.formatDouble())

    internal fun getMotes() = getGroup(ScoreboardPattern.motesPattern, ScoreboardData.sidebarLinesFormatted, "motes") ?: "0"

    internal fun getSoulflow() = getGroup(ScoreboardPattern.soulflowPattern, ScoreboardData.sidebarLinesFormatted, "soulflow") ?: "0"

    internal fun getPurseEarned() =
        getGroup(PurseAPI.coinsPattern, ScoreboardData.sidebarLinesFormatted, "earned")?.let { " §7(§e+$it§7)§6" }

    internal fun getBank() = getGroup(ScoreboardPattern.bankPattern, ScoreboardData.sidebarLinesFormatted, "bank") ?: "0"

    internal fun getBits() = formatNumber(BitsAPI.bits.coerceAtLeast(0))

    internal fun getBitsToClaim() = formatNumber(BitsAPI.bitsAvailable.coerceAtLeast(0))

    internal fun getBitsLine() = if (displayConfig.showUnclaimedBits) {
        "§b${getBits()}§7/§b${getBitsToClaim()}"
    } else "§b${getBits()}"

    internal fun getCopper() =
        getGroup(ScoreboardPattern.copperPattern, ScoreboardData.sidebarLinesFormatted, "copper") ?: "0"

    internal fun getGems() = getGroup(ScoreboardPattern.gemsPattern, ScoreboardData.sidebarLinesFormatted, "gems") ?: "0"

    internal fun getHeat() = getGroup(ScoreboardPattern.heatPattern, ScoreboardData.sidebarLinesFormatted, "heat")

    internal fun getNorthStars() = getGroup(ScoreboardPattern.northstarsPattern, ScoreboardData.sidebarLinesFormatted, "northStars") ?: "0"

    internal fun getElementFromAny(element: Any): ScoreboardElementType = when (element) {
        is String -> element to HorizontalAlignment.LEFT
        is Pair<*, *> -> element.first as String to element.second as HorizontalAlignment
        else -> HIDDEN to HorizontalAlignment.LEFT
    }

    internal fun getSbLines(): List<String> {
        return ScoreboardData.sidebarLinesFormatted
    }

    internal fun getMotes() = getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.motesPattern, "motes")
    internal fun getBank() = getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.bankPattern, "bank")

    internal fun getBits() = BitsAPI.bits.coerceAtLeast(0).formatNum()

    internal fun getBitsToClaim() = BitsAPI.bitsAvailable.coerceAtLeast(0).formatNum()

    internal fun getBitsLine() = if (displayConfig.showUnclaimedBits) {
        "§b${getBits()}§7/§b${getBitsToClaim()}"
    } else {
        "§b${getBits()}"
    }

    internal fun getCopper() =
        getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.copperPattern, "copper")

    internal fun getGems() = getGroupFromPattern(TabListData.getTabList(), ScoreboardPattern.gemsPattern, "gems")

    internal fun getHeat() =
        getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.heatPattern, "heat")

    internal fun getNorthStars() =
        getGroupFromPattern(ScoreboardData.sidebarLinesFormatted, ScoreboardPattern.northstarsPattern, "northstars")


    class UndetectedScoreboardLines(message: String) : Exception(message)
}
