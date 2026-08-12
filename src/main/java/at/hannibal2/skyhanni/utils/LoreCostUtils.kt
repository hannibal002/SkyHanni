package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object LoreCostUtils {

    /**
     * One line of an item cost lore.
     *
     * [internalName] is [NeuInternalName.MISSING_ITEM] when the line could not be read.
     * [rawLine] is the lore line this entry was read from, unchanged.
     */
    data class LoreCostEntry(val internalName: NeuInternalName, val amount: Long, val rawLine: String)

    fun isCostHeader(line: String): Boolean = costHeaderPattern.matches(line)

    private val patternGroup = RepoPattern.group("utils.lore.cost")

    /**
     * REGEX-TEST: §7Cost
     * REGEX-TEST: §5§o§7Cost
     * REGEX-TEST: §7Cost: §b5,000 Bits
     */
    private val costHeaderPattern by patternGroup.pattern(
        "header",
        "(?:§5§o)?§7Cost(?:: (?<inline>.+))?",
    )

    fun SafeItemStack.readLoreCosts(): List<LoreCostEntry> = getLoreComponent()
        .map { it.formattedTextCompatLessResets() }
        .readLoreCosts(hoverName.formattedTextCompatLeadingWhiteLessResets())

    fun List<String>.readLoreCosts(itemName: String = "<unknown>"): List<LoreCostEntry> {
        val headerIndex = indexOfLast { costHeaderPattern.matches(it) }.takeIf { it != -1 } ?: return emptyList()

        costHeaderPattern.matchMatcher(this[headerIndex]) {
            groupOrNull("inline")?.let { return listOf(readCostLine(it, itemName)) }
        }

        return drop(headerIndex + 1).takeWhile { it.isNotEmpty() }.map { readCostLine(it, itemName) }
    }

    private fun readCostLine(rawLine: String, itemName: String): LoreCostEntry {
        // some lines write the amount as "Gold medal§8 x2" instead of "Gold medal §8x2"
        val line = rawLine.replace("§8 ", " §8")

        readCurrencyOrNull(line, rawLine)?.let { return it }

        val (name, amount) = ItemUtils.readItemAmount(line) ?: run {
            logCostLineError("Could not read the amount of a cost line", rawLine, itemName)
            return LoreCostEntry(NeuInternalName.MISSING_ITEM, 1, rawLine)
        }

        val internalName = NeuInternalName.fromItemNameOrNull(name) ?: run {
            logCostLineError("Unknown item in a cost line", rawLine, itemName)
            NeuInternalName.MISSING_ITEM
        }
        return LoreCostEntry(internalName, amount.toLong(), rawLine)
    }

    private fun logCostLineError(reason: String, rawLine: String, itemName: String) {
        ErrorManager.logErrorStateWithData(
            "Could not read the cost of an item",
            reason,
            "rawLine" to rawLine,
            "itemName" to itemName,
            "inventoryName" to InventoryUtils.openInventoryName(),
            betaOnly = true,
        )
    }

    private fun readCurrencyOrNull(line: String, rawLine: String): LoreCostEntry? =
        SkyblockCurrency.readCurrencyOrNull(line)?.let { (currency, amount) ->
            LoreCostEntry(currency.internalName, amount, rawLine)
        }
}
