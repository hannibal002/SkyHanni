package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.config.features.gui.customscoreboard.DisplayConfig
import at.hannibal2.skyhanni.data.BitsAPI
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.features.bingo.BingoAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.displayConfig
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
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

    internal fun getSoulflow() = TabWidget.SOULFLOW.matchMatcherFirstLine { group("amount") } ?: "0"

    internal fun getPurseEarned() =
        getGroup(PurseAPI.coinsPattern, ScoreboardData.sidebarLinesFormatted, "earned")?.let { " §7(§e+$it§7)§6" }

    internal fun getBank() = TabWidget.BANK.matchMatcherFirstLine {
        group("amount") + (groupOrNull("personal")?.let { "§7 / §6$it" } ?: "")
    } ?: "0"

    internal fun getBits() = formatNumber(BitsAPI.bits.coerceAtLeast(0))

    internal fun getBitsToClaim() = formatNumber(BitsAPI.bitsAvailable.coerceAtLeast(0))

    internal fun getBitsLine() = if (displayConfig.showUnclaimedBits) {
        "§b${getBits()}§7/§b${getBitsToClaim()}"
    } else "§b${getBits()}"

    internal fun getCopper() =
        getGroup(ScoreboardPattern.copperPattern, ScoreboardData.sidebarLinesFormatted, "copper") ?: "0"

    internal fun getGems() = TabWidget.GEMS.matchMatcherFirstLine { group("gems") } ?: "0"

    internal fun getHeat() = getGroup(ScoreboardPattern.heatPattern, ScoreboardData.sidebarLinesFormatted, "heat")

    internal fun getNorthStars() = getGroup(ScoreboardPattern.northstarsPattern, ScoreboardData.sidebarLinesFormatted, "northStars") ?: "0"

    internal fun getTablistEvent() = TabWidget.EVENT.matchMatcherFirstLine { group("event") }

    internal fun getElementFromAny(element: Any): ScoreboardElementType = when (element) {
        is String -> element to HorizontalAlignment.LEFT
        is Pair<*, *> -> element.first as String to element.second as HorizontalAlignment
        else -> HIDDEN to HorizontalAlignment.LEFT
    }

    internal fun getSbLines(): List<String> {
        return ScoreboardData.sidebarLinesFormatted
    }

    class UndetectedScoreboardLines(message: String) : Exception(message)
}
