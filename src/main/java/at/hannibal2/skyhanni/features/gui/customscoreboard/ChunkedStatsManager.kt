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
        showWhen = { !(hideEmptyLines && PurseAPI.currentPurse.toInt() == 0) && ScoreboardElementManager.PURSE.element.showWhen() },
        showIsland = { ScoreboardElementManager.PURSE.element.showIsland() },
        configLine = "§6Purse",
    ),
    MOTES(
        displayPair = { "§b${getMotes()}" },
        showWhen = { !(hideEmptyLines && getMotes() == "0") && ScoreboardElementManager.MOTES.element.showWhen() },
        showIsland = { ScoreboardElementManager.MOTES.element.showIsland() },
        configLine = "§dMotes",
    ),
    BANK(
        displayPair = { "§6${getBank()}" },
        showWhen = {
            !(hideEmptyLines && (getBank() == "0" || getBank() == "0§7 / §60")) && ScoreboardElementManager.BANK.element.showWhen()
        },
        showIsland = { ScoreboardElementManager.BANK.element.showIsland() },
        configLine = "§6Bank",
    ),
    BITS(
        displayPair = { getBitsLine() },
        showWhen = { !(hideEmptyLines && getBits() == "0" && getBitsToClaim() == "0") && ScoreboardElementManager.BITS.element.showWhen() },
        showIsland = { ScoreboardElementManager.BITS.element.showIsland() },
        configLine = "§bBits",
    ),
    COPPER(
        displayPair = { "§c${getCopper()}" },
        showWhen = { !(hideEmptyLines && getCopper() == "0") && ScoreboardElementManager.COPPER.element.showWhen() },
        showIsland = { ScoreboardElementManager.COPPER.element.showIsland() },
        configLine = "§cCopper",
    ),
    GEMS(
        displayPair = { "§a${getGems()}" },
        showWhen = { !(hideEmptyLines && getGems() == "0") && ScoreboardElementManager.GEMS.element.showWhen() },
        showIsland = { ScoreboardElementManager.GEMS.element.showIsland() },
        configLine = "§aGems",
    ),
    HEAT(
        displayPair = { "§c${getHeat()}" },
        showWhen = { !(hideEmptyLines && getHeat() == "§c♨ 0") && ScoreboardElementManager.HEAT.element.showWhen() },
        showIsland = { ScoreboardElementManager.HEAT.element.showIsland() },
        configLine = "§cHeat",
    ),
    COLD(
        displayPair = { "§b${MiningAPI.cold}❄" },
        showWhen = { !(hideEmptyLines && MiningAPI.cold == 0) && ScoreboardElementManager.COLD.element.showWhen() },
        showIsland = { ScoreboardElementManager.COLD.element.showIsland() },
        configLine = "§bCold",
    ),
    NORTH_STARS(
        displayPair = { "§d${getNorthStars()}" },
        showWhen = { !(hideEmptyLines && getNorthStars() == "0") && ScoreboardElementManager.NORTH_STARS.element.showWhen() },
        showIsland = { ScoreboardElementManager.NORTH_STARS.element.showIsland() },
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
