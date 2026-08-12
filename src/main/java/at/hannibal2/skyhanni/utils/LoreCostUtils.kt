package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object LoreCostUtils {

    /**
     * One line of an item cost lore.
     *
     * [internalName] is [NeuInternalName.MISSING_ITEM] when the line could not be read.
     */
    data class LoreCostEntry(val internalName: NeuInternalName, val amount: Long)

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

    /**
     * REGEX-TEST: §b5,000 Bits
     * REGEX-TEST: §240 Pests
     * REGEX-TEST: §63,000,000,000 Chocolate
     * REGEX-TEST: §c250 Copper
     * REGEX-TEST: §61,940,000 Coins
     */
    private val currencyPattern by patternGroup.pattern(
        "currency",
        "(?:§.)*(?<amount>[\\d,]+) (?<name>[\\w' ]+)",
    )

    fun SafeItemStack.readLoreCosts(): List<LoreCostEntry> =
        getLoreComponent().map { it.formattedTextCompatLessResets() }.readLoreCosts()

    fun List<String>.readLoreCosts(): List<LoreCostEntry> {
        val headerIndex = indexOfFirst { costHeaderPattern.matches(it) }.takeIf { it != -1 } ?: return emptyList()

        costHeaderPattern.matchMatcher(this[headerIndex]) {
            groupOrNull("inline")?.let { return listOf(readCostLine(it)) }
        }

        return drop(headerIndex + 1).takeWhile { it.isNotEmpty() }.map { readCostLine(it) }
    }

    private fun readCostLine(rawLine: String): LoreCostEntry {
        // some lines write the amount as "Gold medal§8 x2" instead of "Gold medal §8x2"
        val line = rawLine.replace("§8 ", " §8")

        readCurrencyOrNull(line)?.let { return it }

        val (name, amount) = ItemUtils.readItemAmount(line) ?: run {
            ErrorManager.logErrorStateWithData(
                "Could not read a cost line",
                "readItemAmount failed on a lore cost line",
                "rawLine" to rawLine,
                betaOnly = true,
            )
            return LoreCostEntry(NeuInternalName.MISSING_ITEM, 1)
        }
        return LoreCostEntry(NeuInternalName.fromItemName(name), amount.toLong())
    }

    private fun readCurrencyOrNull(line: String): LoreCostEntry? = currencyPattern.matchMatcher(line) {
        val currency = SkyblockCurrency.getByLoreNameOrNull(group("name")) ?: return@matchMatcher null
        LoreCostEntry(currency.internalName, group("amount").formatLong())
    }
}
