package at.hannibal2.skyhanni.features.gui.customscoreboard

import at.hannibal2.skyhanni.data.MiningAPI
import at.hannibal2.skyhanni.data.PurseAPI
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.chunkedConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboard.informationFilteringConfig
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.formatNumber
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBank
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBits
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsAvailable
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getBitsLine
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getCopper
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getGems
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getHeat
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getMotes
import at.hannibal2.skyhanni.features.gui.customscoreboard.CustomScoreboardUtils.getNorthStars
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Bank
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Bits
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Cold
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Copper
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Gems
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Heat
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Motes
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.NorthStars
import at.hannibal2.skyhanni.features.gui.customscoreboard.elements.Purse

private val hideEmptyLines get() = informationFilteringConfig.hideEmptyLines

enum class ChunkedStatsManager(
    private val displayPair: () -> String,
    val showWhen: () -> Boolean,
    val showIsland: () -> Boolean,
    private val configLine: String,
) {
    PURSE(
        displayPair = { "§6${formatNumber(PurseAPI.currentPurse)}" },
        showWhen = { !(hideEmptyLines && PurseAPI.currentPurse.toInt() == 0) && Purse.showWhen() },
        showIsland = { Purse.showIsland() },
        configLine = "§6Purse",
    ),
    MOTES(
        displayPair = { "§b${getMotes()}" },
        showWhen = { !(hideEmptyLines && getMotes() == "0") && Motes.showWhen() },
        showIsland = { Motes.showIsland() },
        configLine = "§dMotes",
    ),
    BANK(
        displayPair = { "§6${getBank()}" },
        showWhen = { !(hideEmptyLines && (getBank() == "0" || getBank() == "0§7 / §60")) && Bank.showWhen() },
        showIsland = { Bank.showIsland() },
        configLine = "§6Bank",
    ),
    BITS(
        displayPair = { getBitsLine() },
        showWhen = { !(hideEmptyLines && getBits() == "0" && getBitsAvailable() == "0") && Bits.showWhen() },
        showIsland = { Bits.showIsland() },
        configLine = "§bBits",
    ),
    COPPER(
        displayPair = { "§c${getCopper()}" },
        showWhen = { !(hideEmptyLines && getCopper() == "0") && Copper.showWhen() },
        showIsland = { Copper.showIsland() },
        configLine = "§cCopper",
    ),
    GEMS(
        displayPair = { "§a${getGems()}" },
        showWhen = { !(hideEmptyLines && getGems() == "0") && Gems.showWhen() },
        showIsland = { Gems.showIsland() },
        configLine = "§aGems",
    ),
    HEAT(
        displayPair = { "§c${getHeat()}" },
        showWhen = { !(hideEmptyLines && getHeat() == "§c♨ 0") && Heat.showWhen() },
        showIsland = { Heat.showIsland() },
        configLine = "§cHeat",
    ),
    COLD(
        displayPair = { "§b${MiningAPI.cold}❄" },
        showWhen = { !(hideEmptyLines && MiningAPI.cold == 0) && Cold.showWhen() },
        showIsland = { Cold.showIsland() },
        configLine = "§bCold",
    ),
    NORTH_STARS(
        displayPair = { "§d${getNorthStars()}" },
        showWhen = { !(hideEmptyLines && getNorthStars() == "0") && NorthStars.showWhen() },
        showIsland = { NorthStars.showIsland() },
        configLine = "§dNorth Stars",
    ),
    ;

    override fun toString() = configLine

    companion object {

        private var currentIslandStats = listOf<ChunkedStatsManager>()

        fun getChunkedStats() = buildList {
            currentIslandStats.forEach { stat ->
                if (stat.showWhen()) {
                    add(stat.displayPair.invoke())
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
