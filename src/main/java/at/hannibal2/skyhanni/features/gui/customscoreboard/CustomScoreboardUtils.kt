package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.features.gui.customscoreboard.DisplayConfig
import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.HypixelData
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
import java.util.regex.Pattern

object CustomScoreboardUtils {

    internal fun Pattern.getGroup(list: List<String>, group: String) =
        list.map { it.removeResets().trimWhiteSpace() }.firstNotNullOfOrNull { line ->
            matchGroup(line, group)
        }

    fun getProfileTypeSymbol() = when {
        HypixelData.ironman -> "§7♲ "
        HypixelData.stranded -> "§a☀ "
        HypixelData.bingo -> ScoreboardData.sidebarLinesFormatted.firstNotNullOfOrNull {
            BingoAPI.getIconFromScoreboard(it)?.plus(" ")
        } ?: "§e❤ "

        else -> "§e"
    }

    // TODO change to a non extended function
    internal fun Number.formatNum(): String = when (displayConfig.numberFormat) {
        DisplayConfig.NumberFormat.SHORT -> this.shortFormat()
        DisplayConfig.NumberFormat.LONG -> addSeparators()
        else -> "0"
    }

    internal fun String.formatNum() = formatDouble().formatNum()

    internal fun getMotes() = ScoreboardPattern.motesPattern.getGroup(ScoreboardData.sidebarLinesFormatted, "motes") ?: "0"

    internal fun getBank() = ScoreboardPattern.bankPattern.getGroup(ScoreboardData.sidebarLinesFormatted, "bank") ?: "0"

    internal fun getBits() = BitsAPI.bits.coerceAtLeast(0).formatNum()

    internal fun getBitsToClaim() = BitsAPI.bitsAvailable.coerceAtLeast(0).formatNum()

    internal fun getBitsLine() = if (displayConfig.showUnclaimedBits) {
        "§b${getBits()}§7/§b${getBitsToClaim()}"
    } else "§b${getBits()}"

    internal fun getCopper() =
        ScoreboardPattern.copperPattern.getGroup(ScoreboardData.sidebarLinesFormatted, "copper") ?: "0"

    internal fun getGems() = ScoreboardPattern.gemsPattern.getGroup(ScoreboardData.sidebarLinesFormatted, "gems") ?: "0"

    internal fun getHeat() = ScoreboardPattern.heatPattern.getGroup(ScoreboardData.sidebarLinesFormatted, "heat")

    internal fun getNorthStars() = ScoreboardPattern.northstarsPattern.getGroup(ScoreboardData.sidebarLinesFormatted, "northStars") ?: "0"

    internal fun Any.getElementFromAny(): ScoreboardElementType = when (this) {
        is String -> this to HorizontalAlignment.LEFT
        is Pair<*, *> -> this.first as String to this.second as HorizontalAlignment
        else -> HIDDEN to HorizontalAlignment.LEFT
    }

    class UndetectedScoreboardLines(message: String) : Exception(message)
}
