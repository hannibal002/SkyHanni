package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.chunkedConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber
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

enum class ChunkedStatsManager(
    private val displayPair: Supplier<String>,
    val showWhen: () -> Boolean,
    val showIsland: () -> Boolean,
    private val configLine: String,
) {
    PURSE(
        displayPair = { "§6${formatNumber(PurseAPI.currentPurse)}" },
        showWhen = { !(hideEmptyLines && PurseAPI.currentPurse.toInt() == 0) && ScoreboardEntry.PURSE.element.showWhen() },
        showIsland = { ScoreboardEntry.PURSE.element.showIsland() },
        configLine = "§6Purse",
    ),
    MOTES(
        displayPair = { "§b${getMotes()}" },
        showWhen = { !(hideEmptyLines && getMotes() == "0") && ScoreboardEntry.MOTES.element.showWhen() },
        showIsland = { ScoreboardEntry.MOTES.element.showIsland() },
        configLine = "§dMotes",
    ),
    BANK(
        displayPair = { "§6${getBank()}" },
        showWhen = {
            !(hideEmptyLines && (getBank() == "0" || getBank() == "0§7 / §60")) && ScoreboardEntry.BANK.element.showWhen()
        },
        showIsland = { ScoreboardEntry.BANK.element.showIsland() },
        configLine = "§6Bank",
    ),
    BITS(
        displayPair = { getBitsLine() },
        showWhen = { !(hideEmptyLines && getBits() == "0" && getBitsToClaim() == "0") && ScoreboardEntry.BITS.element.showWhen() },
        showIsland = { ScoreboardEntry.BITS.element.showIsland() },
        configLine = "§bBits",
    ),
    COPPER(
        displayPair = { "§c${getCopper()}" },
        showWhen = { !(hideEmptyLines && getCopper() == "0") && ScoreboardEntry.COPPER.element.showWhen() },
        showIsland = { ScoreboardEntry.COPPER.element.showIsland() },
        configLine = "§cCopper",
    ),
    GEMS(
        displayPair = { "§a${getGems()}" },
        showWhen = { !(hideEmptyLines && getGems() == "0") && ScoreboardEntry.GEMS.element.showWhen() },
        showIsland = { ScoreboardEntry.GEMS.element.showIsland() },
        configLine = "§aGems",
    ),
    HEAT(
        displayPair = { "§c${getHeat()}" },
        showWhen = { !(hideEmptyLines && getHeat() == "§c♨ 0") && ScoreboardEntry.HEAT.element.showWhen() },
        showIsland = { ScoreboardEntry.HEAT.element.showIsland() },
        configLine = "§cHeat",
    ),
    COLD(
        displayPair = { "§b${MiningAPI.cold}❄" },
        showWhen = { !(hideEmptyLines && MiningAPI.cold == 0) && ScoreboardEntry.COLD.element.showWhen() },
        showIsland = { ScoreboardEntry.COLD.element.showIsland() },
        configLine = "§bCold",
    ),
    NORTH_STARS(
        displayPair = { "§d${getNorthStars()}" },
        showWhen = { !(hideEmptyLines && getNorthStars() == "0") && ScoreboardEntry.NORTH_STARS.element.showWhen() },
        showIsland = { ScoreboardEntry.NORTH_STARS.element.showIsland() },
        configLine = "§dNorth Stars",
    ),
    ;

    override fun toString() = configLine

    companion object {

        private var currentIslandStats = listOf<ChunkedStatsManager>()

        fun getChunkedStats() = buildList {
            currentIslandStats.forEach { stat ->
                if (stat.showWhen()) {
                    add(stat.displayPair.get())
                }
            }
        }

        fun shouldShowChunkedStats() = currentIslandStats.any { it.showWhen() }

        fun showChunkedStatsIsland(): Boolean {
            currentIslandStats = chunkedConfig.chunkedStats.filter { it.showIsland() }
            return currentIslandStats.isNotEmpty()
        }
    }
}
