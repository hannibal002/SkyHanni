package at.hannibal2.skyhanni.config.enums

enum class NoTradeModeSetting(private val displayName: String) {

    NONE("§cNone"),
    NO_TRADE("§7No Trade Profiles"),
    ALL_PROFILES("§2All Profiles"),
    ;

    override fun toString(): String = displayName
}
