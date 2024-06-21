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
import java.util.regex.Pattern

object CustomScoreboardUtils {

    fun getGroup(pattern: Pattern, list: List<String>, group: String) =
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

    fun formatNumber(number: Number): String = when (displayConfig.numberFormat) {
        DisplayConfig.NumberFormat.SHORT -> number.shortFormat()
        DisplayConfig.NumberFormat.LONG -> number.addSeparators()
        else -> "0"
    }

    fun formatStringNum(string: String) = formatNumber(string.formatDouble())

    fun getMotes() = getGroup(ScoreboardPattern.motesPattern, ScoreboardData.sidebarLinesFormatted, "motes") ?: "0"

    fun getSoulflow() = getGroup(ScoreboardPattern.soulflowPattern, ScoreboardData.sidebarLinesFormatted, "soulflow") ?: "0"

    fun getPurseEarned() = getGroup(PurseAPI.coinsPattern, ScoreboardData.sidebarLinesFormatted, "earned")?.let { " §7(§e+$it§7)§6" }

    fun getBank() = getGroup(ScoreboardPattern.bankPattern, ScoreboardData.sidebarLinesFormatted, "bank") ?: "0"

    fun getBits() = formatNumber(BitsAPI.bits.coerceAtLeast(0))

    fun getBitsToClaim() = formatNumber(BitsAPI.bitsAvailable.coerceAtLeast(0))

    fun getBitsLine() = if (displayConfig.showUnclaimedBits) {
        "§b${getBits()}§7/§b${getBitsToClaim()}"
    } else "§b${getBits()}"

    fun getCopper() =
        getGroup(ScoreboardPattern.copperPattern, ScoreboardData.sidebarLinesFormatted, "copper") ?: "0"

    fun getGems() = getGroup(ScoreboardPattern.gemsPattern, ScoreboardData.sidebarLinesFormatted, "gems") ?: "0"

    fun getHeat() = getGroup(ScoreboardPattern.heatPattern, ScoreboardData.sidebarLinesFormatted, "heat")

    fun getNorthStars() = getGroup(ScoreboardPattern.northstarsPattern, ScoreboardData.sidebarLinesFormatted, "northStars") ?: "0"

    fun getElementFromAny(element: Any): ScoreboardElementType = when (element) {
        is String -> element to HorizontalAlignment.LEFT
        is Pair<*, *> -> element.first as String to element.second as HorizontalAlignment
        else -> HIDDEN to HorizontalAlignment.LEFT
    }

    class UndetectedScoreboardLines(message: String) : Exception(message)
}
