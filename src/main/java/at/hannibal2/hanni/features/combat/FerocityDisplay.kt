package at.hannibal2.hanni.features.combat

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.model.SkyblockStat
import at.hannibal2.hanni.events.GuiRenderEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RenderUtils.renderString
import at.hannibal2.hanni.utils.SkyBlockUtils

@HanniModule
object FerocityDisplay {

    private val config get() = HanniMod.feature.combat.ferocityDisplay

    @HandleEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        SkyblockStat.FEROCITY.displayValue?.let {
            config.position.renderString(it, posLabel = "Ferocity Display")
        }
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
