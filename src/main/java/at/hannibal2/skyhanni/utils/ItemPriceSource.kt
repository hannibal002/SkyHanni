package at.hannibal2.skyhanni.utils

enum class ItemPriceSource(val displayName: String) {
    BAZAAR_INSTANT_BUY("Instant Buy"), // Sell Offer
    BAZAAR_INSTANT_SELL("Instant Sell"), // Buy Order
    NPC_SELL("NPC Sell"),
    ;

    override fun toString(): String = displayName
}
