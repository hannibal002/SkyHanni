package at.hannibal2.skyhanni.events.bazaar

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

class BazaarTransactionEvent(
    val transactionType: TransactionType,
    val coinAmount: Double,
    val coinAmountAfterTax: Double,
) : SkyHanniEvent() {
    enum class TransactionType(private val message: String) {
        INSTANT_BUY("Bought"),
        BUY_ORDER("Buy Order Setup!"),
        INSTANT_SELL("Sold"),
        SELL_OFFER("Sell Offer Setup!"),
        FLIP_ORDER("Order Flipped!"),
        ;

        companion object {
            fun getByMessageOrNull(message: String) = entries.firstOrNull { it.message == message }
        }
    }
}
