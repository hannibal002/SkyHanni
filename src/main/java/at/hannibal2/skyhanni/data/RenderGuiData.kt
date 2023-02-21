package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.GuiRenderEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class RenderGuiData {

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        GuiRenderEvent(GuiRenderEvent.RenderType.IN_WORLD).postAndCatch()
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiScreenEvent.BackgroundDrawnEvent) {
        GuiRenderEvent(GuiRenderEvent.RenderType.INVENTORY_BACKGROUND).postAndCatch()
    }
}