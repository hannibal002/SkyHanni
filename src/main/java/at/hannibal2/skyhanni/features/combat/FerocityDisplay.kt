package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString

@SkyHanniModule
object FerocityDisplay {

    private val config get() = SkyHanniMod.feature.combat.ferocityDisplay

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (SkyblockStat.FEROCITY.lastKnownValue == 0.0) return
        config.position.renderString(SkyblockStat.FEROCITY.displayValue, posLabel = "Ferocity Display")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled
}
