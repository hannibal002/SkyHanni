package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
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

    /** True when the item can be bought right now, as opposed to a locked or already owned entry. */
    fun List<String>.hasTradeLine(): Boolean = any { tradeLinePattern.matches(it.removeColor()) }

    private val patternGroup = RepoPattern.group("utils.lore")

    /**
     * This is one of the few patterns that keeps its color codes on purpose. The cost written
     * into the header line is handed on as is, so that features can find that exact line again
     * in the tooltip. Matching without color would strip it and break that lookup.
     *
     * REGEX-TEST: §7Cost
     * REGEX-TEST: §5§o§7Cost
     * REGEX-TEST: §7Cost: §b5,000 Bits
     * REGEX-TEST: §7Cost to unlock: §550 Tokens
     */
    private val costHeaderPattern by patternGroup.pattern(
        "cost.header",
        "(?:§.)*Cost(?: to unlock)?(?:: (?<cost>.+))?",
    )

    /**
     * Shops word this line differently, the essence perk shops unlock and the chip menu levels
     * up, but all of them list their cost the same way. Kuudra names the mouse button, and its
     * preview line uses the right button, which must not count as a trade.
     *
     * REGEX-TEST: Click to trade!
     * REGEX-TEST: Click to unlock!
     * REGEX-TEST: Click to level up!
     * REGEX-TEST: Left Click to unlock!
     * REGEX-FAIL: Right Click to preview!
     */
    private val tradeLinePattern by patternGroup.pattern(
        "trade.click",
        "(?:Left )?Click to (?:trade|unlock|level up)!",
    )

    /**
     * REGEX-TEST: 50 Safari Essence
     * REGEX-TEST: 50,000 Undead Essence
     */
    private val amountFirstPattern by patternGroup.pattern(
        "cost.amount-first",
        "(?<amount>[\\d,]+) (?<name>[\\w' ]+)",
    )

    fun SafeItemStack.readLoreCosts(): List<LoreCostEntry> = getLoreComponent()
        .map { it.formattedTextCompatLessResets() }
        .readLoreCosts(hoverName.formattedTextCompatLeadingWhiteLessResets())

    fun List<String>.readLoreCosts(itemName: String = "<unknown>"): List<LoreCostEntry> {
        val headerIndex = indexOfLast { costHeaderPattern.matches(it) }.takeIf { it != -1 } ?: return emptyList()

        costHeaderPattern.matchMatcher(this[headerIndex]) {
            groupOrNull("cost")?.let { return listOf(readCostLine(it, itemName)) }
        }

        return drop(headerIndex + 1).takeWhile { it.isNotEmpty() }.map { readCostLine(it, itemName) }
    }

    private fun readCostLine(rawLine: String, itemName: String): LoreCostEntry {
        // some lines write the amount as "Gold medal§8 x2" instead of "Gold medal §8x2"
        val line = rawLine.replace("§8 ", " §8")

        readCurrencyOrNull(line, rawLine)?.let { return it }
        readAmountFirstOrNull(line, rawLine)?.let { return it }

        val (name, amount) = ItemUtils.readItemAmount(line) ?: run {
            logCostLineError("Could not read the amount of a cost line", rawLine, itemName)
            return LoreCostEntry(NeuInternalName.MISSING_ITEM, 1, rawLine)
        }

        // currencies without a repo item only arrive here, their amount is written behind the name or not at all
        val internalName = NeuInternalName.fromItemNameOrNull(name)
            ?: SkyblockCurrency.getByLoreNameOrNull(name)?.internalName
            ?: run {
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

    /**
     * Items that write the amount in front of the name, like "50,000 Undead Essence".
     * [ItemUtils.readItemAmount] only reads the other form, where the amount is behind the name.
     */
    private fun readAmountFirstOrNull(line: String, rawLine: String): LoreCostEntry? =
        amountFirstPattern.matchMatcher(line.removeColor()) {
            val internalName = NeuInternalName.fromItemNameOrNull(group("name")) ?: return@matchMatcher null
            LoreCostEntry(internalName, group("amount").formatLong(), rawLine)
        }
}
