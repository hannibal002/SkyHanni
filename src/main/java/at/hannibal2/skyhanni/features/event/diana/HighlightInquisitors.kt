package at.hannibal2.skyhanni.features.event.diana

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.diana.InquisitorFoundEvent
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColorInt
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object HighlightInquisitors {

    private val config get() = SkyHanniMod.feature.event.diana

    @SubscribeEvent
    fun onInquisitorFound(event: InquisitorFoundEvent) {
        if (!config.highlightInquisitors) return

        val inquisitor = event.inquisitorEntity

        val color = config.color.toChromaColorInt()
        RenderLivingEntityHelper.setEntityColorWithNoHurtTime(inquisitor, color) { config.highlightInquisitors }
    }
}
