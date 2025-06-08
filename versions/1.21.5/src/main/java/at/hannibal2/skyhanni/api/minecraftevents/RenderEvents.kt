package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer
import net.fabricmc.fabric.api.client.rendering.v1.LayeredDrawerWrapper
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.Identifier


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

        // GameOverlayRenderPreEvent
        // todo need to post the rest of these + cancelling
        HudLayerRegistrationCallback.EVENT.register(
            HudLayerRegistrationCallback { layeredDrawer: LayeredDrawerWrapper ->
                layeredDrawer.attachLayerBefore(
                    IdentifiedLayer.HOTBAR_AND_BARS,
                    makeLayer("hotbar_pre"),
                    this::postHotbarLayerEventPre,
                )
            },
        )

        HudLayerRegistrationCallback.EVENT.register(
            HudLayerRegistrationCallback { layeredDrawer: LayeredDrawerWrapper ->
                layeredDrawer.attachLayerAfter(
                    IdentifiedLayer.HOTBAR_AND_BARS,
                    makeLayer("hotbar_post"),
                    this::postHotbarLayerEventPost,
                )
            },
        )



        // GameOverlayRenderPostEvent

        // GuiScreenOpenEvent

        // GuiMouseInputEvent

        // BlockOverlayRenderEvent

        // GuiActionPerformedEvent

        // InitializeGuiEvent

    }

    private fun postHotbarLayerEventPre(context: DrawContext, ticks: RenderTickCounter) {
        GameOverlayRenderPreEvent(context, RenderLayer.HOTBAR).post()
    }

    private fun postHotbarLayerEventPost(context: DrawContext, ticks: RenderTickCounter) {
        GameOverlayRenderPostEvent(context, RenderLayer.HOTBAR).post()
    }

    private fun makeLayer(name: String): Identifier {
        return Identifier.of("skyhanni", name)
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
