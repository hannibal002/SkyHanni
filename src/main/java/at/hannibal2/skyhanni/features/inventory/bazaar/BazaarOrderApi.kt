package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.bazaar.BazaarOrdersLoadedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * Keeps track of the bazaar orders the player currently has open.
 *
 * The order inventory is the authoritative source and replaces the stored state completely. It is
 * titled "Your Bazaar Orders" without a co-op and "Co-op Bazaar Orders" with one, and both are
 * complete as far as the player's own orders go.
 * Chat messages only bridge the gap between two visits of that inventory, and only for buy
 * orders. Sell offers change exclusively on an inventory visit.
 */
@SkyHanniModule
object BazaarOrderApi {

    private val patternGroup = RepoPattern.group("bazaar.orders")

    private val storage get() = ProfileStorageData.profileSpecific?.bazaarOrders
    private val buyOrders get() = storage?.buyOrders
    private val sellOffers get() = storage?.sellOffers

    /**
     * REGEX-TEST: §a§lBUY §fWheat
     * REGEX-TEST: §a§lBUY §aMagma Slug Shard
     * REGEX-TEST: §a§lBUY §dWither Essence
     */
    private val itemNamePattern by patternGroup.pattern(
        "itemname",
        "§.§l(?<type>BUY|SELL) (?<name>.*)",
    )

    /**
     * REGEX-TEST: Order amount: 50x
     * REGEX-TEST: Order amount: 2,000x
     * REGEX-TEST: Offer amount: 50x
     */
    private val amountPattern by patternGroup.pattern(
        "amount.colorless",
        "(?:Order|Offer) amount: (?<amount>[\\d,]+)x",
    )

    /**
     * The bazaar omits this line entirely while nothing of the order has been traded yet.
     *
     * REGEX-TEST: Filled: 26/50 (52%)
     * REGEX-TEST: Filled: 59/59 100%!
     */
    private val filledPattern by patternGroup.pattern(
        "filled.colorless",
        "Filled: (?<filled>[\\d,]+)/[\\d,]+ .*",
    )

    /**
     * REGEX-TEST: You have 6 items to claim!
     * REGEX-TEST: You have 20 items to claim!
     */
    private val claimablePattern by patternGroup.pattern(
        "claimable.colorless",
        "You have (?<amount>[\\d,]+) items? to claim!",
    )

    /**
     * REGEX-TEST: Price per unit: 521.7 coins
     * REGEX-TEST: Price per unit: 2,082.3 coins
     */
    private val pricePerUnitPattern by patternGroup.pattern(
        "price.colorless",
        "Price per unit: (?<price>[\\d,.]+) coins",
    )

    /**
     * REGEX-TEST: By: [MVP+] hannibal2
     * REGEX-TEST: By: hannibal2
     */
    private val ownerPattern by patternGroup.pattern(
        "owner.colorless",
        "By: (?:\\[[^]]+] )?(?<name>\\S+)",
    )

    /**
     * REGEX-TEST: [Bazaar] Buy Order Setup! 1x Coal for 4.4 coins.
     * REGEX-TEST: [Bazaar] Buy Order Setup! 50x Enchanted Iron Ingot for 26,085 coins.
     */
    private val orderSetupPattern by patternGroup.pattern(
        "chat.setup.colorless",
        "\\[Bazaar] Buy Order Setup! (?<amount>[\\d,]+)x (?<item>.*) for [\\d,.]+ coins\\.",
    )

    /**
     * REGEX-TEST: [Bazaar] Claimed 24x Nessie Shard worth 4.8M coins bought for 202k each!
     */
    private val claimedPattern by patternGroup.pattern(
        "chat.claimed.colorless",
        "\\[Bazaar] Claimed (?<amount>[\\d,]+)x (?<item>.*) worth .* coins bought for .* each!",
    )

    /**
     * REGEX-TEST: [Bazaar] Cancelled! Refunded 12,345 coins from cancelling Buy Order!
     */
    val cancelledPattern by patternGroup.pattern(
        "chat.cancelled.colorless",
        "\\[Bazaar] Cancelled! Refunded (?<coins>.*) coins from cancelling Buy Order!",
    )

    private val ordersInventory = InventoryDetector(
        checkInventoryName = { BazaarApi.isBazaarOrderInventory(it) },
        onOpenInventory = { readOrders(it.inventoryItems) },
    )

    fun inOrderInventory(): Boolean = ordersInventory.isInside()

    // Claiming or cancelling happens inside the open menu, which never reopens it.
    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!ordersInventory.isInside()) return
        readOrders(event.inventoryItems)
    }

    private fun readOrders(inventoryItems: Map<Int, SafeItemStack>) {
        val orders = parseInventory(inventoryItems)
        syncOwnOrders(orders)
        BazaarOrdersLoadedEvent(orders).post()
    }

    private fun parseInventory(inventoryItems: Map<Int, SafeItemStack>): List<BazaarOrder> =
        inventoryItems.mapNotNull { (slot, stack) -> parseOrder(slot, stack) }

    private fun parseOrder(slot: Int, stack: SafeItemStack): BazaarOrder? {
        val name = stack.hoverName.formattedTextCompatLeadingWhiteLessResets()
        val (type, itemName) = itemNamePattern.matchMatcher(name) {
            val type = when (group("type")) {
                "BUY" -> BazaarApi.SimpleTransactionType.BUY_ORDER
                else -> BazaarApi.SimpleTransactionType.SELL_OFFER
            }
            type to group("name")
        } ?: return null

        val lore = stack.getCleanLore()
        // The bazaar omits the filled and claimable lines while nothing has been traded yet,
        // and the owner line whenever the player has no co-op.
        val filled = filledPattern.firstMatcher(lore) { group("filled").formatInt() } ?: 0
        val claimable = claimablePattern.firstMatcher(lore) { group("amount").formatInt() } ?: 0
        val owner = ownerPattern.firstMatcher(lore) { group("name") }

        val amount = amountPattern.firstMatcher(lore) { group("amount").formatInt() }
        val pricePerUnit = pricePerUnitPattern.firstMatcher(lore) { group("price").formatDouble() }
        if (amount == null || pricePerUnit == null) {
            ErrorManager.logErrorStateWithData(
                "Could not read a bazaar order",
                "Bazaar order is missing a line that should always be present",
                "missing amount" to (amount == null),
                "missing price per unit" to (pricePerUnit == null),
                "item name" to name,
                "lore" to lore,
            )
            return null
        }

        return BazaarOrder(
            slot = slot,
            type = type,
            internalName = NeuInternalName.fromItemName(itemName),
            amount = amount,
            filled = filled,
            claimable = claimable,
            pricePerUnit = pricePerUnit,
            // Without a co-op the inventory shows no owner, and every order in it is the player's.
            isOwn = owner == null || owner == PlayerUtils.getName(),
        )
    }

    private fun syncOwnOrders(orders: List<BazaarOrder>) {
        val buy = mutableMapOf<NeuInternalName, Int>()
        val sell = mutableMapOf<NeuInternalName, Int>()
        for (order in orders) {
            if (!order.isOwn) continue
            val outstanding = order.outstanding
            if (outstanding <= 0) continue
            when (order.type) {
                BazaarApi.SimpleTransactionType.BUY_ORDER -> buy.addOrPut(order.internalName, outstanding)
                BazaarApi.SimpleTransactionType.SELL_OFFER -> sell.addOrPut(order.internalName, outstanding)
            }
        }
        val storage = storage ?: return
        storage.buyOrders = buy
        storage.sellOffers = sell
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onChat(event: SkyHanniChatEvent.Allow) {
        val message = event.cleanMessage

        orderSetupPattern.matchMatcher(message) {
            val internalName = NeuInternalName.fromItemNameOrNull(group("item")) ?: return
            buyOrders?.addOrPut(internalName, group("amount").formatInt())
            return
        }

        claimedPattern.matchMatcher(message) {
            val internalName = NeuInternalName.fromItemNameOrNull(group("item")) ?: return
            removeClaimed(internalName, group("amount").formatInt())
            return
        }

        cancelledPattern.matchMatcher(message) {
            // The exact order is unknown when multiple orders exist for the same item.
            // Dropping all of them only ever makes the mod suggest buying something again,
            // which is far less harmful than hiding something the player still needs.
            val internalName = BazaarApi.orderOptionProduct ?: return
            buyOrders?.remove(internalName)
            return
        }
    }

    private fun removeClaimed(internalName: NeuInternalName, amount: Int) {
        val orders = buyOrders ?: return
        val newAmount = (orders[internalName] ?: return) - amount
        if (newAmount > 0) orders[internalName] = newAmount else orders.remove(internalName)
    }

    fun getOpenAmount(internalName: NeuInternalName, type: BazaarApi.SimpleTransactionType): Int {
        val orders = when (type) {
            BazaarApi.SimpleTransactionType.BUY_ORDER -> buyOrders
            BazaarApi.SimpleTransactionType.SELL_OFFER -> sellOffers
        }
        return orders?.get(internalName) ?: 0
    }

    @HandleEvent
    private fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Bazaar Orders")

        val storage = storage
        if (storage == null) {
            event.addIrrelevant("no profile storage")
            return
        }
        if (storage.buyOrders.isEmpty() && storage.sellOffers.isEmpty()) {
            event.addIrrelevant("no open orders")
            return
        }

        event.addIrrelevant {
            add("buy orders: ${storage.buyOrders.size}")
            for ((internalName, amount) in storage.buyOrders) {
                add("  ${internalName.asString()}: $amount")
            }
            add("sell offers: ${storage.sellOffers.size}")
            for ((internalName, amount) in storage.sellOffers) {
                add("  ${internalName.asString()}: $amount")
            }
        }
    }
}
