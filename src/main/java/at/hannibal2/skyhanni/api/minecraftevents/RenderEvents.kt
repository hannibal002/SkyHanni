package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.data.RenderData
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
//#if MC < 1.21.6
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer
//#else
//$$ import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
//$$ import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
//#endif
//#if MC < 1.21.9
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.renderer.MultiBufferSource
import com.mojang.blaze3d.vertex.PoseStack

//#endif

@SkyHanniModule
object RenderEvents {

    init {

        // SkyHanniRenderWorldEvent
        //#if MC < 1.21.9
        WorldRenderEvents.AFTER_TRANSLUCENT.register { event ->
            val immediateVertexConsumers = event.consumers() as? MultiBufferSource.BufferSource ?: return@register
            val stack = event.matrixStack() ?: PoseStack()
            SkyHanniRenderWorldEvent(
                stack,
                event.camera(),
                immediateVertexConsumers,
                event.tickCounter().getGameTimeDeltaPartialTick(true),
            ).post()
        }
        //#endif

        // ScreenDrawnEvent

        // GuiScreenOpenEvent

        // GuiMouseInputEvent

        // BlockOverlayRenderEvent

        // GuiActionPerformedEvent

        // InitializeGuiEvent

        //#if MC < 1.21.6
        HudLayerRegistrationCallback.EVENT.register { context ->
            context.attachLayerAfter(
                IdentifiedLayer.SLEEP,
                ResourceLocation.fromNamespaceAndPath("skyhanni", "gui_render_layer"),
                RenderEvents::postGui,
            )
        }
        //#else
        //$$ HudElementRegistry.attachElementBefore(
        //$$     VanillaHudElements.SLEEP,
        //$$     ResourceLocation.fromNamespaceAndPath("skyhanni", "gui_render_layer"),
        //$$     RenderEvents::postGui
        //$$ )
        //#endif
    }

    private fun postGui(context: GuiGraphics, tick: DeltaTracker) {
        if (Minecraft.getInstance().options.hideGui) return
        RenderData.postRenderOverlay(context)
    }

    // GameOverlayRenderPreEvent
    // todo need to post the rest of these, sadly fapi doesn't have the same layers as 1.8 does
    @JvmStatic
    fun postHotbarLayerEventPre(context: GuiGraphics): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.HOTBAR).post()
    }

    @JvmStatic
    fun postExperienceBarLayerEventPre(context: GuiGraphics): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.EXPERIENCE_BAR).post()
    }

    @JvmStatic
    fun postExperienceNumberLayerEventPre(context: GuiGraphics): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.EXPERIENCE_NUMBER).post()
    }

    @JvmStatic
    fun postTablistLayerEventPre(context: GuiGraphics): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.PLAYER_LIST).post()
    }

    // GameOverlayRenderPostEvent
    // todo need to post the rest of these, sadly fapi doesn't have the same layers as 1.8 does
    @JvmStatic
    fun postHotbarLayerEventPost(context: GuiGraphics) {
        GameOverlayRenderPostEvent(context, RenderLayer.HOTBAR).post()
    }

    @JvmStatic
    fun postExperienceBarLayerEventPost(context: GuiGraphics) {
        GameOverlayRenderPostEvent(context, RenderLayer.EXPERIENCE_BAR).post()
    }

    @JvmStatic
    fun postExperienceNumberLayerEventPost(context: GuiGraphics) {
        GameOverlayRenderPostEvent(context, RenderLayer.EXPERIENCE_NUMBER).post()
    }
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
}
