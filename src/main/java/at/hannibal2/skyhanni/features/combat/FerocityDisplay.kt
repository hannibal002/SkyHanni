package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object FerocityDisplay {

    private val config get() = SkyHanniMod.feature.combat.ferocityDisplay

    @HandleEvent
    fun onGuiRenderOverlay() {
        if (!isEnabled()) return
        SkyblockStat.FEROCITY.displayValue?.let {
            config.position.renderRenderable(Renderable.text(it), posLabel = "Ferocity Display")
        }
    }
}
