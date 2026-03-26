package at.hannibal2.skyhanni.features.gui.moveablehud

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MoveableHeldItemTooltip : MoveableHudOverlay(
    RenderLayer.HELD_ITEM_TOOLTIP,
    displayName = "Held Item Tooltip",
    width = 182,
    height = 10,
    anchorOffsetX = 91,
    anchorOffsetY = 59,
) {
    override val config get() = SkyHanniMod.feature.gui.heldItemTooltip

    @HandleEvent(priority = HandleEvent.LOWEST)
    override fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) = super.onRenderOverlayPre(event)

    @HandleEvent(priority = HandleEvent.HIGHEST)
    override fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) = super.onRenderOverlayPost(event)
}
