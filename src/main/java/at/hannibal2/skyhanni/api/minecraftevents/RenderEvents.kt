package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.events.GuiKeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.render.BlockOverlayRenderEvent
import at.hannibal2.skyhanni.events.render.OverlayType
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
import at.hannibal2.skyhanni.utils.compat.DrawContext
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat
import at.hannibal2.skyhanni.utils.compat.WorldRenderContext
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
        if (!canRender()) return
        if (!SkyHanniDebugsAndTests.globalRender) return
        SkyHanniRenderWorldEvent(WorldRenderContext(), event.partialTicks).post()
    }

    @SubscribeEvent
    fun onGuiRender(event: DrawScreenEvent.Post) {
        if (!canRender()) return
        ScreenDrawnEvent(DrawContext(), event.gui).post()
    }

    @SubscribeEvent
    fun onPostRenderTick(event: RenderTickEvent) {
        if (!canRender()) return
        RenderingTickEvent(DrawContext(), event.phase == TickEvent.Phase.START).post()
    }

    @SubscribeEvent
    fun onRenderOverlayPre(event: RenderGameOverlayEvent.Pre) {
        if (!canRender()) return
        if (GameOverlayRenderPreEvent(DrawContext(), RenderLayer.fromForge(event.type)).post()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onRenderOverlayPost(event: RenderGameOverlayEvent.Post) {
        if (!canRender()) return
        GameOverlayRenderPostEvent(DrawContext(), RenderLayer.fromForge(event.type)).post()
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
        if (GuiMouseInputEvent(event.gui).post()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onRenderBlockOverlay(event: RenderBlockOverlayEvent) {
        if (BlockOverlayRenderEvent(OverlayType.fromForge(event.overlayType)).post()) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onBackgroundDraw(event: GuiScreenEvent.BackgroundDrawnEvent) {
        if (!canRender()) return
        DrawBackgroundEvent(DrawContext()).post()
    }

    @SubscribeEvent
    fun onGuiAction(event: GuiScreenEvent.ActionPerformedEvent.Post) {
        GuiActionPerformedEvent(event.gui, event.button).post()
    }

    @SubscribeEvent
    fun onGuiInitPost(event: GuiScreenEvent.InitGuiEvent.Post) {
        InitializeGuiEvent(event.gui, event.buttonList).post()
    }

    private fun canRender(): Boolean = MinecraftCompat.localWorldExists && MinecraftCompat.localPlayerExists
}

enum class RenderLayer {
    ALL,
    HELMET,
    PORTAL,
    CROSSHAIRS,
    BOSSHEALTH,
    ARMOR,
    HEALTH,
    FOOD,
    AIR,
    HOTBAR,
    EXPERIENCE_BAR,
    TEXT,
    HEALTHMOUNT,
    JUMPBAR,
    CHAT,
    PLAYER_LIST,
    DEBUG,
    // Not a real forge layer but is used on modern Minecraft versions
    EXPERIENCE_NUMBER,
    ;

    companion object {
        fun fromForge(element: RenderGameOverlayEvent.ElementType): RenderLayer {
            return entries[element.ordinal]
        }
    }
}
