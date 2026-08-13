package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.TabWidget
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.WidgetUpdateEvent
import at.hannibal2.skyhanni.events.inventory.NpcTradeEvent
import at.hannibal2.skyhanni.features.gui.customscoreboard.ScoreboardPattern
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
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

    private val storage get() = ProfileStorageData.profileSpecific?.currencies


    private fun SkyblockCurrency.setAmount(amount: Long) {
        storage?.put(this, amount)
    }

    fun SkyblockCurrency.getFromStorage(): Long? = storage?.get(this)

    // reading every line instead of only the new ones, so the amount is still picked up
    // when the profile storage loads after the scoreboard
    @HandleEvent(onlyOnSkyblock = true)
    private fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        for (line in event.new) {
            val message = line.trimWhiteSpace().removeResets()

            ScoreboardPattern.copperPattern.matchMatcher(message) {
                SkyblockCurrency.COPPER.setAmount(group("copper").formatLong())
            }
            // while sowdust is gained, hypixel shortens the number, those lines are skipped on purpose
            ScoreboardPattern.sowdustPattern.matchMatcher(message) {
                SkyblockCurrency.SOWDUST.setAmount(group("sowdust").formatLong())
            }
            ScoreboardPattern.gemsPattern.matchMatcher(message) {
                SkyblockCurrency.GEMS.setAmount(group("gems").formatLong())
            }

            // the pattern is shared with the custom scoreboard, a repo override may still lack the group
            ScoreboardPattern.peltsPattern.matchMatcher(message) {
                groupOrNull("pelts")?.formatLongOrNull()?.let { SkyblockCurrency.PELTS.setAmount(it) }
            }
            readCleanLine(message.removeColor())
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
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        for (item in event.inventoryItems.values) {
            // tony's shop names one of its items after the amount the player owns
            peltsAmountPattern.matchMatcher(item.hoverName.string.removeColor()) {
                SkyblockCurrency.PELTS.setAmount(group("amount").formatLong())
            }
            for (line in item.getLoreComponent().map { it.string.removeColor() }) {
                readCleanLine(line)
            }
        }
    }

    /**
     * Currencies with a live source of their own, like coins or bits, are skipped: they are not in
     * the storage and reading them already accounts for the purchase.
     */
    @HandleEvent
    private fun onNpcTrade(event: NpcTradeEvent) {
        for (cost in event.costs) {
            val currency = SkyblockCurrency.getByInternalNameOrNull(cost.internalName) ?: continue
            val owned = currency.getFromStorage() ?: continue
            currency.setAmount((owned - cost.amount * event.amount).coerceAtLeast(0))
        }
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
    }

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
            for ((currency, amount) in storage.orEmpty()) {
                add("$currency: $amount")
            }
        }
    }
}
