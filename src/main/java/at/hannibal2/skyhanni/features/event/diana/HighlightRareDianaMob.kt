package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.diana.RareDianaMobFoundEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toColor

@SkyHanniModule
object HighlightRareDianaMob {

    private val config get() = SkyHanniMod.feature.event.diana

    @HandleEvent
    fun onRareDianaMobFound(event: RareDianaMobFoundEvent) {
        if (!config.highlightInquisitors) return

        val rareMob = event.entity

        val color = config.color.toColor()
        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(rareMob, color) { config.highlightInquisitors }
    }
}
