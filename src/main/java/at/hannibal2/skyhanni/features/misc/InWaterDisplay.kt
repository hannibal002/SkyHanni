package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

@SkyHanniModule
object InWaterDisplay {

    private val config get() = SkyHanniMod.feature.misc.stranded
    private val inWaterRenderable = Renderable.text("§7In Water: §aTrue")
    private val outOfWaterRenderable = Renderable.text("§7In Water: §cFalse")

    @HandleEvent(onlyOnSkyblock = true)
    fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.inWaterDisplay) return

        val display = if (MinecraftCompat.localPlayer.isInWater) inWaterRenderable else outOfWaterRenderable
        config.inWaterPosition.renderRenderable(display, posLabel = "In Water Display")
    }
}
