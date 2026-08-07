package at.hannibal2.skyhanni.features.inventory.bazaar

import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getOpenBuyOrderAmount
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getOpenSellOfferAmount
import at.hannibal2.skyhanni.utils.NeuInternalName

/**
 * A single entry in a bazaar order inventory, not an aggregate over an item.
 *
 * A player can hold several orders for the same product, at different prices.
 * Multiple instances can therefore share the same [internalName].
 *
 * For the total amount of an item, use [BazaarApi.getOpenBuyOrderAmount] or [BazaarApi.getOpenSellOfferAmount].
 *
 * @property amount how many units this single order was placed for.
 * @property filled how many units the bazaar traded so far, claimed or not.
 * @property claimable how many traded units still wait to be picked up.
 * @property isOwn whether the order belongs to the player, not to a co-op member.
 */
data class BazaarOrder(
    val slot: Int,
    val type: BazaarApi.SimpleTransactionType,
    val internalName: NeuInternalName,
    val amount: Int,
    val filled: Int,
    val claimable: Int,
    val pricePerUnit: Double,
    val isOwn: Boolean,
) {
    /**
     * Items that are still owed by this order and are not in the player's possession yet.
     * Everything that was already claimed is counted somewhere else, for example in the hunting box.
     */
    val outstanding: Int get() = (amount - (filled - claimable)).coerceAtLeast(0)
}
