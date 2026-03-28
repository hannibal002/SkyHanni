package at.hannibal2.skyhanni.features.gui.moveablehud

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MoveableXPBar : MoveableHudOverlay(
    RenderLayer.EXPERIENCE_BAR, RenderLayer.EXPERIENCE_NUMBER,
    displayName = "XP Bar",
    width = 182,
    height = 5,
    anchorOffsetX = 91,
    anchorOffsetY = 29,
) {
    override val config get() = SkyHanniMod.feature.gui.xpBar

    @HandleEvent(priority = HandleEvent.LOWEST)
    override fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) = super.onRenderOverlayPre(event)

    @HandleEvent(priority = HandleEvent.HIGHEST)
    override fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) = super.onRenderOverlayPost(event)
}
