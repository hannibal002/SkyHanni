package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.hotx.CurrencyPerHotxPerk
import at.hannibal2.skyhanni.data.hotx.HotfData
import at.hannibal2.skyhanni.data.hotx.HotxData
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators

@SkyHanniModule
object WhispersPerHotfPerk : CurrencyPerHotxPerk<HotfData.Companion>(HotfData, "Whispers") {

    private val config get() = SkyHanniMod.feature.foraging.hotf

    @HandleEvent
    fun onToolTip(event: ToolTipTextEvent) {
        if (!isEnabled()) return
        handleHotxCurrency(
            event,
            config.whispersSpent,
            config.whispersFor10Levels,
            config.currentWhispers,
            config.whispersSpentDesign,
        )
    }

    override fun currentCurrencyLineString(perk: HotxData<*>): String {
        return "§3${HotfData.whispersCurrent.addSeparators()} Forest Whispers"
    }

    override fun isEnabled() = super.isEnabled() && (config.whispersSpent || config.whispersFor10Levels || config.currentWhispers)
}
