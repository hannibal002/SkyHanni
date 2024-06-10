package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.MiningAPI.getCold
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.chunkedConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNum
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBank
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBits
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsAvailable
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsLine
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getCopper
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getGems
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getHeat
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getMotes
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getNorthStars
import java.util.function.Supplier

val hideEmptyLines get() = informationFilteringConfig.hideEmptyLines

enum class ChunkedStats(
    private val displayPair: Supplier<String>,
    val showWhen: () -> Boolean,
    private val configLine: String,
) {
    PURSE(
        {
            "§6${PurseAPI.currentPurse.formatNum()}"
        },
        {
            !(hideEmptyLines && PurseAPI.currentPurse.toInt() == 0) && ScoreboardElement.PURSE.showWhen()
        },
        "§6Purse",
    ),
    MOTES(
        {
            "§b${getMotes()}"
        },
        {
            !(hideEmptyLines && getMotes() == "0") && ScoreboardElement.MOTES.showWhen()
        },
        "§dMotes",
    ),
    BANK(
        {
            "§6${getBank()}"
        },
        {
            !(hideEmptyLines && (getBank() == "0" || getBank() == "0§7 / §60")) && ScoreboardElement.BANK.showWhen()
        },
        "§6Bank",
    ),
    BITS(
        {
            getBitsLine()
        },
        {
            !(hideEmptyLines && getBits() == "0" && getBitsAvailable() == "0") && ScoreboardElement.BITS.showWhen()
        },
        "§bBits",
    ),
    COPPER(
        {
            "§c${getCopper()}"
        },
        {
            !(hideEmptyLines && getCopper() == "0") && ScoreboardElement.COPPER.showWhen()
        },
        "§cCopper",
    ),
    GEMS(
        {
            "§a${getGems()}"
        },
        {
            !(hideEmptyLines && getGems() == "0") && ScoreboardElement.GEMS.showWhen()
        },
        "§aGems",
    ),
    HEAT(
        {
            "§c${getHeat()}"
        },
        {
            !(hideEmptyLines && getHeat() == "§c♨ 0") && ScoreboardElement.HEAT.showWhen()
        },
        "§cHeat",
    ),
    COLD(
        {
            "§b${getCold()} ❄"
        },
        {
            !(hideEmptyLines && getCold() == 0) && ScoreboardElement.COLD.showWhen()
        },
        "§bCold",
    ),
    NORTH_STARS(
        {
            "§d${getNorthStars()}"
        },
        {
            !(hideEmptyLines && getNorthStars() == "0") && ScoreboardElement.NORTH_STARS.showWhen()
        },
        "§dNorth Stars",
    ),
    ;

    override fun toString() = configLine


    fun getDisplay() = displayPair.get()

    companion object {
        fun getChunkedStats() = buildList {
            for (stat in chunkedConfig.chunkedStats) {
                if (stat.showWhen()) {
                    add(stat.getDisplay())
                }
            }
        }

        fun shouldShowChunkedStats() = chunkedConfig.chunkedStats.any { it.showWhen() }
    }
}
