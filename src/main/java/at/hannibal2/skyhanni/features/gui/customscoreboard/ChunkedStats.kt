package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.MiningAPI.getCold
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.chunkedConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNum
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBank
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBits
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsLine
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsToClaim
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getCopper
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getGems
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getHeat
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getMotes
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getNorthStars
import java.util.function.Supplier

private val hideEmptyLines get() = informationFilteringConfig.hideEmptyLines

enum class ChunkedStats(
    private val displayPair: Supplier<String>,
    val showWhen: () -> Boolean,
    private val configLine: String,
) {
    PURSE(
        displayPair = { "§6${PurseAPI.currentPurse.formatNum()}" },
        showWhen = { !(hideEmptyLines && PurseAPI.currentPurse.toInt() == 0) && ScoreboardElement.PURSE.showWhen() },
        configLine = "§6Purse"
    ),
    MOTES(
        displayPair = { "§b${getMotes()}" },
        showWhen = { !(hideEmptyLines && getMotes() == "0") && ScoreboardElement.MOTES.showWhen() },
        configLine = "§dMotes"
    ),
    BANK(
        displayPair = { "§6${getBank()}" },
        showWhen = { !(hideEmptyLines && (getBank() == "0" || getBank() == "0§7 / §60")) && ScoreboardElement.BANK.showWhen() },
        configLine = "§6Bank"
    ),
    BITS(
        displayPair = { getBitsLine() },
        showWhen = { !(hideEmptyLines && getBits() == "0" && getBitsToClaim() == "0") && ScoreboardElement.BITS.showWhen() },
        configLine = "§bBits"
    ),
    COPPER(
        displayPair = { "§c${getCopper()}" },
        showWhen = { !(hideEmptyLines && getCopper() == "0") && ScoreboardElement.COPPER.showWhen() },
        configLine = "§cCopper"
    ),
    GEMS(
        displayPair = { "§a${getGems()}" },
        showWhen = { !(hideEmptyLines && getGems() == "0") && ScoreboardElement.GEMS.showWhen() },
        configLine = "§aGems"
    ),
    HEAT(
        displayPair = { "§c${getHeat()}" },
        showWhen = { !(hideEmptyLines && getHeat() == "§c♨ 0") && ScoreboardElement.HEAT.showWhen() },
        configLine = "§cHeat"
    ),
    COLD(
        displayPair = { "§b${getCold()} ❄" },
        showWhen = { !(hideEmptyLines && getCold() == 0) && ScoreboardElement.COLD.showWhen() },
        configLine = "§bCold"
    ),
    NORTH_STARS(
        displayPair = { "§d${getNorthStars()}" },
        showWhen = { !(hideEmptyLines && getNorthStars() == "0") && ScoreboardElement.NORTH_STARS.showWhen() },
        configLine = "§dNorth Stars"
    );

    override fun toString() = configLine

    fun getDisplay() = displayPair.get()

    companion object {
        fun getChunkedStats() = buildList {
            chunkedConfig.chunkedStats.forEach { stat ->
                if (stat.showWhen()) {
                    add(stat.getDisplay())
                }
            }
        }

        fun shouldShowChunkedStats() = chunkedConfig.chunkedStats.any { it.showWhen() }
    }
}
