package at.hannibal2.skyhanni.events.bazaar

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

class BazaarTransactionEvent(
    val transactionType: TransactionType,
    val coinAmount: Double,
    val coinAmountAfterTax: Double,
) : SkyHanniEvent() {
    enum class TransactionType {
        INSTANT_BUY,
        BUY_ORDER,
        INSTANT_SELL,
        SELL_OFFER,
        FLIP_ORDER
    }
}
