package at.hannibal2.hanni.features.event.diana

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.diana.InquisitorFoundEvent
import at.hannibal2.hanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toColor

@HanniModule
object HighlightInquisitors {

    private val config get() = HanniMod.feature.event.diana

    @HandleEvent
    fun onInquisitorFound(event: InquisitorFoundEvent) {
        if (!config.highlightInquisitors) return

        val inquisitor = event.inquisitorEntity

        val color = config.color.toColor()
        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(inquisitor, color) { config.highlightInquisitors }
    }
}
