package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hotx.CurrencyPerHotxPerk
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.data.hotx.HotxData
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators

@SkyHanniModule
object PowderPerHotmPerk : CurrencyPerHotxPerk<HotmData.Companion>(HotmData, "Powder") {

    private val config get() = SkyHanniMod.feature.mining.hotm

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return
        handleHotxCurrency(
            event,
            config.powderSpent,
            config.powderFor10Levels,
            config.currentPowder,
            config.powderSpentDesign,
        )
    }

    override fun currentCurrencyLineString(perk: HotxData<*>): String? {
        if (perk !is HotmData) return null
        val powderType = perk.powderType ?: return null
        return "${powderType.color}${powderType.current.addSeparators()} ${powderType.displayName} Powder"
    }

    override fun isEnabled() = super.isEnabled() && (config.powderSpent || config.powderFor10Levels || config.currentPowder)
}
