package at.hannibal2.hanni.api.minecraftevents

import at.hannibal2.hanni.data.RenderData
import at.hannibal2.hanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.hanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.util.Identifier
//#if MC < 1.21.6
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer
//#else
//$$ import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
//$$ import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
//#endif
//#if MC < 1.21.9
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
//#endif

@HanniModule
object RenderEvents {

    init {

        // HanniRenderWorldEvent
        //#if MC < 1.21.9
        WorldRenderEvents.AFTER_TRANSLUCENT.register { event ->
            val immediateVertexConsumers = event.consumers() as? VertexConsumerProvider.Immediate ?: return@register
            val stack = event.matrixStack() ?: MatrixStack()
            HanniRenderWorldEvent(stack, event.camera(), immediateVertexConsumers, event.tickCounter().getTickProgress(true)).post()
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
            context.attachLayerAfter(IdentifiedLayer.SLEEP, Identifier.of("hanni", "gui_render_layer"), RenderEvents::postGui)
        }
        //#else
        //$$ HudElementRegistry.attachElementBefore(
        //$$     VanillaHudElements.SLEEP,
        //$$     Identifier.of("hanni", "gui_render_layer"),
        //$$     RenderEvents::postGui
        //$$ )
        //#endif
    }

    private fun postGui(context: DrawContext, tick: RenderTickCounter) {
        if (MinecraftClient.getInstance().options.hudHidden) return
        RenderData.postRenderOverlay(context)
    }

    // GameOverlayRenderPreEvent
    // todo need to post the rest of these, sadly fapi doesn't have the same layers as 1.8 does
    @JvmStatic
    fun postHotbarLayerEventPre(context: DrawContext): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.HOTBAR).post()
    }

    @JvmStatic
    fun postExperienceBarLayerEventPre(context: DrawContext): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.EXPERIENCE_BAR).post()
    }

    @JvmStatic
    fun postExperienceNumberLayerEventPre(context: DrawContext): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.EXPERIENCE_NUMBER).post()
    }

    @JvmStatic
    fun postTablistLayerEventPre(context: DrawContext): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.PLAYER_LIST).post()
    }

    // GameOverlayRenderPostEvent
    // todo need to post the rest of these, sadly fapi doesn't have the same layers as 1.8 does
    @JvmStatic
    fun postHotbarLayerEventPost(context: DrawContext) {
        GameOverlayRenderPostEvent(context, RenderLayer.HOTBAR).post()
    }

    @JvmStatic
    fun postExperienceBarLayerEventPost(context: DrawContext) {
        GameOverlayRenderPostEvent(context, RenderLayer.EXPERIENCE_BAR).post()
    }

    @JvmStatic
    fun postExperienceNumberLayerEventPost(context: DrawContext) {
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
