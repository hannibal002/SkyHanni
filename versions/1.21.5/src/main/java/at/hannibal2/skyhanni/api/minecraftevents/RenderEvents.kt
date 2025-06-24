package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack

@SkyHanniModule
object RenderEvents {

    init {

        // SkyHanniRenderWorldEvent
        WorldRenderEvents.AFTER_TRANSLUCENT.register { event ->
            val immediateVertexConsumers = event.consumers() as? VertexConsumerProvider.Immediate ?: return@register
            val stack = event.matrixStack() ?: MatrixStack()
            SkyHanniRenderWorldEvent(stack, event.camera(), immediateVertexConsumers, event.tickCounter().getTickProgress(true)).post()
        }

        // ScreenDrawnEvent

        // GuiScreenOpenEvent

        // GuiMouseInputEvent

        // BlockOverlayRenderEvent

        // GuiActionPerformedEvent

        // InitializeGuiEvent

    }

    // GameOverlayRenderPreEvent
    // todo need to post the rest of these, sadly fapi doesn't have the same layers as 1.8 does
    @JvmStatic
    fun postHotbarLayerEventPre(context: DrawContext): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.HOTBAR).post()
    }

    @JvmStatic
    fun postExperienceLayerEventPre(context: DrawContext): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.EXPERIENCE).post()
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
    fun postExperienceLayerEventPost(context: DrawContext) {
        GameOverlayRenderPostEvent(context, RenderLayer.EXPERIENCE).post()
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
    EXPERIENCE,
    TEXT,
    HEALTHMOUNT,
    JUMPBAR,
    CHAT,
    PLAYER_LIST,
    DEBUG;
}
