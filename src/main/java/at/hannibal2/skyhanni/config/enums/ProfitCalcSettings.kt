package at.hannibal2.skyhanni.config.enums

import at.hannibal2.skyhanni.utils.SkyBlockUtils

enum class ProfitCalcSettings(private val displayName: String) {

    NONE("§cNone"),
    NO_TRADE("§7No Trade Profiles"),
    ALL_PROFILES("§2All Profiles"),
    ;

    override fun toString(): String = displayName

    fun ignoreMaterialCost(): Boolean {
        return this == ALL_PROFILES || (this == NO_TRADE && SkyBlockUtils.noTradeMode)
    }
}
