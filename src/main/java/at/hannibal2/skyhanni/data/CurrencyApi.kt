package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.inventory.NpcTradeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.formatLongOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SkyblockCurrency
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeResets
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpace
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * Remembers how much of a currency the player owns, for currencies that Hypixel only shows on
 * some islands. Without this the amount would be unknown everywhere else.
 */
@SkyHanniModule
object CurrencyApi {

    private val patternGroup = RepoPattern.group("data.currency")

    /**
     * REGEX-TEST: You have: 1,005 Gems
     */
    private val gemsAmountPattern by patternGroup.pattern(
        "gems.amount",
        "You have: (?<amount>[\\d,]+) Gems",
    )

    /**
     * REGEX-TEST: Pelts: 815
     */
    private val peltsAmountPattern by patternGroup.pattern(
        "pelts.amount",
        "Pelts: (?<amount>[\\d,]+)",
    )

    /**
     * REGEX-TEST: Vacuum Bag: 2,161  Pests
     */
    private val pestsAmountPattern by patternGroup.pattern(
        "pests.amount",
        "Vacuum Bag: (?<amount>[\\d,]+) \uE018 Pests",
    )

    /**
     * REGEX-TEST: GOLD medals: 174
     * REGEX-TEST: SILVER medals: 16
     * REGEX-TEST: BRONZE medals: 6
     */
    private val medalAmountPattern by patternGroup.pattern(
        "medal.amount",
        "(?<type>GOLD|SILVER|BRONZE) medals: (?<amount>[\\d,]+)",
    )

    /**
     * The type is read as written, so a new kind of essence needs no code change.
     *
     * REGEX-TEST: Your Undead Essence: 28,439
     * REGEX-TEST: Your Wither Essence: 1,204
     */
    private val essenceAmountPattern by patternGroup.pattern(
        "essence.amount",
        "Your (?<type>[\\w ]+) Essence: (?<amount>[\\d,]+)",
    )

    /**
     * The widget header has no amount and is skipped by the pattern. A shortened number like
     * "17k" is skipped as well, the amount group does not match it.
     *
     * WRAPPED-REGEX-TEST: " Undead: 28,439"
     * WRAPPED-REGEX-TEST: " Crimson: 12"
     */
    private val essenceWidgetPattern by patternGroup.pattern(
        "essence.widget",
        "\\s*(?<type>[\\w ]+): (?<amount>[\\d,]+)",
    )

    /**
     * REGEX-TEST: Copper: 3,416
     */
    val copperScoreboardPattern by patternGroup.pattern(
        "copper.scoreboard.amount",
        "Copper: (?<copper>[\\d,]+).*",
    )

    /**
     * REGEX-TEST: Sowdust: 30,210,307
     * WRAPPED-REGEX-TEST: " Sowdust: 30,120,093"
     */
    val sowdustScoreboardPattern by patternGroup.pattern(
        "sowdust.scoreboard.amount",
        "\\s*Sowdust: (?<sowdust>[\\d,]+).*",
    )

    /**
     * REGEX-TEST: Gems: 350
     */
    val gemsScoreboardPattern by patternGroup.pattern(
        "gems.scoreboard.amount",
        "\\s*Gems: (?<gems>[\\d,]+).*",
    )

    /**
     * REGEX-TEST: Motes: 137,242
     */
    private val motesScoreboardPattern by patternGroup.pattern(
        "motes.scoreboard.amount",
        "\\s*Motes: (?<motes>[\\d,]+).*",
    )

    /**
     * REGEX-TEST: Pelts: 160
     */
    val peltsScoreboardPattern by patternGroup.pattern(
        "pelts.scoreboard.amount",
        "\\s*Pelts: (?<pelts>[\\d,]+).*",
    )

    /**
     * REGEX-TEST: Tokens: 65
     */
    val tokensScoreboardPattern by patternGroup.pattern(
        "tokens.amount",
        "\\s*Tokens: (?<tokens>[\\d,]+).*",
    )

    private val profileStorage get() = ProfileStorageData.profileSpecific?.currencies
    private val accountStorage get() = ProfileStorageData.playerSpecific?.currencies
    private val essenceStorage get() = ProfileStorageData.profileSpecific?.essences

    private val SkyblockCurrency.storage get() = if (accountWide) accountStorage else profileStorage

    private fun SkyblockCurrency.setAmount(amount: Long) {
        storage?.put(this, amount)
    }

    fun SkyblockCurrency.getFromStorage(): Long? = storage?.get(this)

    // reading every line instead of only the new ones, so the amount is still picked up
    // when the profile storage loads after the scoreboard
    @HandleEvent(onlyOnSkyblock = true)
    private fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        for (line in event.new) {
            val message = line.trimWhiteSpace().removeResets().removeColor()

            copperScoreboardPattern.matchMatcher(message) {
                SkyblockCurrency.COPPER.setAmount(group("copper").formatLong())
            }
            // while sowdust is gained, hypixel shortens the number, those lines are skipped on purpose
            sowdustScoreboardPattern.matchMatcher(message) {
                SkyblockCurrency.SOWDUST.setAmount(group("sowdust").formatLong())
            }
            gemsScoreboardPattern.matchMatcher(message) {
                SkyblockCurrency.GEMS.setAmount(group("gems").formatLong())
            }
            motesScoreboardPattern.matchMatcher(message) {
                SkyblockCurrency.MOTES.setAmount(group("motes").formatLong())
            }

            // these patterns are shared with the custom scoreboard, a repo override may still lack the group
            peltsScoreboardPattern.matchMatcher(message) {
                groupOrNull("pelts")?.formatLongOrNull()?.let { SkyblockCurrency.PELTS.setAmount(it) }
            }
            // the group also matches shortened numbers, those are dropped by formatLongOrNull
            tokensScoreboardPattern.matchMatcher(message) {
                groupOrNull("tokens")?.formatLongOrNull()?.let { SkyblockCurrency.KUUDRA_TOKEN.setAmount(it) }
            }
            readCleanLine(message)
        }
    }

    /**
     * The widget groups match anything, so the amount is neither guaranteed to be a number nor to be
     * written out. Hypixel shortens gems to "1k", and a rounded amount is worse than none at all.
     */
    private fun String.exactAmountOrNull(): Long? =
        removeColor().takeIf { it.isNotEmpty() && it.all { char -> char.isDigit() || char == ',' } }?.formatLongOrNull()

    @HandleEvent(onlyOnSkyblock = true)
    private fun onWidgetUpdate(event: WidgetUpdateEvent) {
        when {
            event.isWidget(TabWidget.COPPER) -> TabWidget.COPPER.matchMatcherFirstLine {
                group("copper").exactAmountOrNull()?.let { SkyblockCurrency.COPPER.setAmount(it) }
            }

            event.isWidget(TabWidget.SOWDUST) -> TabWidget.SOWDUST.matchMatcherFirstLine {
                group("sowdust").exactAmountOrNull()?.let { SkyblockCurrency.SOWDUST.setAmount(it) }
            }

            event.isWidget(TabWidget.GEMS) -> TabWidget.GEMS.matchMatcherFirstLine {
                group("gems").exactAmountOrNull()?.let { SkyblockCurrency.GEMS.setAmount(it) }
            }

            event.isWidget(TabWidget.ESSENCE) -> readEssenceWidget(event.cleanLines)
        }
    }

    /** The widget lists one line per essence type below its header. */
    private fun readEssenceWidget(lines: List<String>) {
        for (line in lines) {
            essenceWidgetPattern.matchMatcher(line) {
                // the pattern is generic, a line that is no essence belongs to an unknown widget
                val internalName = essenceInternalNameOrNull(group("type")) ?: return@matchMatcher
                essenceStorage?.put(internalName, group("amount").formatLong())
            }
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        for (item in event.inventoryItems.values) {
            // Tony's Shop names one of its items after the amount the player owns
            peltsAmountPattern.matchMatcher(item.cleanName) {
                SkyblockCurrency.PELTS.setAmount(group("amount").formatLong())
            }
            for (line in item.getCleanLore()) {
                readCleanLine(line)
            }
        }
    }

    @HandleEvent
    private fun onNpcTrade(event: NpcTradeEvent) {
        for ((internalName, amount) in event.costs) {
            subtractCost(internalName, amount * event.amount)
        }
    }

    /**
     * Only what is stored here can be corrected. Amounts with a live source of their own, like
     * coins or bits, are skipped, since reading them already accounts for the purchase.
     */
    private fun subtractCost(internalName: NeuInternalName, total: Long) {
        SkyblockCurrency.getByInternalNameOrNull(internalName)?.let { currency ->
            val owned = currency.getFromStorage() ?: return
            currency.setAmount((owned - total).coerceAtLeast(0))
            return
        }
        val essences = essenceStorage ?: return
        val owned = essences[internalName] ?: return
        essences[internalName] = (owned - total).coerceAtLeast(0)
    }

    // kuudra tokens only exist within a run, every visit starts over at zero
    @HandleEvent
    private fun onWorldChange() {
        SkyblockCurrency.KUUDRA_TOKEN.setAmount(0)
    }

    /** Lines that read the same in an item lore and in the scoreboard, once the colors are gone. */
    private fun readCleanLine(line: String) {
        // the "SkyBlock Gems" item shows up in every menu that sells something for gems
        gemsAmountPattern.matchMatcher(line) {
            SkyblockCurrency.GEMS.setAmount(group("amount").formatLong())
        }
        // the Pesthunter Menu, the vacuum bag is where the pests are kept
        pestsAmountPattern.matchMatcher(line) {
            SkyblockCurrency.PESTS.setAmount(group("amount").formatLong())
        }
        // the Jacob's Farming Contest menu, and the scoreboard while a contest is running
        medalAmountPattern.matchMatcher(line) {
            medalCurrencyOrNull(group("type"))?.setAmount(group("amount").formatLong())
        }
        // every essence shop menu holds an item that states how much of it the player owns
        essenceAmountPattern.matchMatcher(line) {
            val type = group("type")
            val internalName = essenceInternalNameOrNull(type) ?: run {
                ErrorManager.logErrorStateWithData(
                    "Could not read how much essence you own",
                    "Unknown essence type in an item lore",
                    "type" to type,
                    "line" to line,
                    "inventoryName" to InventoryUtils.openInventoryName(),
                    betaOnly = true,
                )
                return@matchMatcher
            }

            essenceStorage?.put(internalName, group("amount").formatLong())
        }
    }

    private fun essenceInternalNameOrNull(type: String): NeuInternalName? =
        NeuInternalName.fromItemNameOrNull("$type Essence")

    /**
     * Essence is a regular repo item, but the player does not carry it in the inventory, so the
     * amount cannot be counted and is remembered from the tab list or from an essence shop menu.
     */
    fun getEssenceOrNull(internalName: NeuInternalName): Long? = essenceStorage?.get(internalName)

    // null is unreachable, the pattern allows nothing else
    private fun medalCurrencyOrNull(type: String) = when (type) {
        "GOLD" -> SkyblockCurrency.GOLD_MEDAL
        "SILVER" -> SkyblockCurrency.SILVER_MEDAL
        "BRONZE" -> SkyblockCurrency.BRONZE_MEDAL
        else -> null
    }

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Currencies")
        event.addIrrelevant {
            add("profile:")
            for ((currency, amount) in profileStorage.orEmpty()) {
                add(" - $currency: $amount")
            }
            add("account:")
            for ((currency, amount) in accountStorage.orEmpty()) {
                add(" - $currency: $amount")
            }
            add("essence:")
            for ((internalName, amount) in essenceStorage.orEmpty()) {
                add(" - ${internalName.asString()}: $amount")
            }
        }
    }
}
