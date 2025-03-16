package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.render.BlockOverlayRenderEvent
import at.hannibal2.skyhanni.events.render.gui.DrawBackgroundEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.events.render.gui.GuiActionPerformedEvent
import at.hannibal2.skyhanni.events.render.gui.GuiMouseInputEvent
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.skyhanni.events.render.gui.InitializeGuiEvent
import at.hannibal2.skyhanni.events.render.gui.RenderingTickEvent
import at.hannibal2.skyhanni.events.render.gui.ScreenDrawnEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.SkyHanniDebugsAndTests
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent
import net.minecraftforge.client.event.RenderBlockOverlayEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent

@SkyHanniModule
object RenderEvents {

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!SkyHanniDebugsAndTests.globalRender) return
        SkyHanniRenderWorldEvent(event.partialTicks).post()
    }

    @SubscribeEvent
    fun onGuiRender(event: DrawScreenEvent.Post) {
        ScreenDrawnEvent(event.gui).post()
    }

    @SubscribeEvent
    fun onPostRenderTick(event: RenderTickEvent) {
        RenderingTickEvent(event.phase == TickEvent.Phase.START).post()
    }

    @SubscribeEvent
    fun onRenderOverlayPre(event: RenderGameOverlayEvent.Pre) {
        if (GameOverlayRenderPreEvent(event.type, event.resolution).post()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onRenderOverlayPost(event: RenderGameOverlayEvent.Post) {
        GameOverlayRenderPostEvent(event.type).post()
    }

    @SubscribeEvent
    fun onGuiOpen(event: GuiOpenEvent) {
        GuiScreenOpenEvent(event.gui).post()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onGuiScreenKeybind(event: GuiScreenEvent.KeyboardInputEvent.Pre) {
        val guiScreen = event.gui as? GuiContainer ?: return
        if (GuiKeyPressEvent(guiScreen).post()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMouseInput(event: GuiScreenEvent.MouseInputEvent.Pre) {
        if (GuiMouseInputEvent().post()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onRenderBlockOverlay(event: RenderBlockOverlayEvent) {
        if (BlockOverlayRenderEvent(event.overlayType).post()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiScreenEvent.BackgroundDrawnEvent) {
        DrawBackgroundEvent.post()
    }

    @SubscribeEvent
    fun onGuiAction(event: GuiScreenEvent.ActionPerformedEvent.Post) {
        GuiActionPerformedEvent(event.gui, event.button).post()
    }

    @SubscribeEvent
    fun onGuiInitPost(event: GuiScreenEvent.InitGuiEvent.Post) {
        InitializeGuiEvent(event.gui, event.buttonList).post()
    }
}
