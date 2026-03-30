package at.hannibal2.skyhanni.features.gui.moveablehud

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MoveableHotBar : MoveableHudOverlay(
    RenderLayer.HOTBAR,
    displayName = "Hotbar",
    width = 182,
    height = 22,
    anchorOffsetX = 91,
    anchorOffsetY = 22,
) {
    override val config get() = SkyHanniMod.feature.gui.hotbar

    @HandleEvent(priority = HandleEvent.LOWEST)
    override fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) = super.onRenderOverlayPre(event)

    @HandleEvent(priority = HandleEvent.HIGHEST)
    override fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) = super.onRenderOverlayPost(event)
}
