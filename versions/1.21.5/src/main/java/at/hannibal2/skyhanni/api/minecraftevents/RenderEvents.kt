package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer
import net.fabricmc.fabric.api.client.rendering.v1.LayeredDrawerWrapper
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderTickCounter
import net.minecraft.util.Identifier


@SkyHanniModule
object RenderEvents {


    private val LAYER: Identifier = Identifier.of("skyhanni", "layer")

    init {

        // SkyHanniRenderWorldEvent

        // ScreenDrawnEvent

        // RenderingTickEvent

        // GameOverlayRenderPreEvent
        HudLayerRegistrationCallback.EVENT.register(
            HudLayerRegistrationCallback { layeredDrawer: LayeredDrawerWrapper ->
                layeredDrawer.attachLayerBefore(
                    IdentifiedLayer.HOTBAR_AND_BARS,
                    LAYER,
                    this::postEvent,
                )
            },
        )


        // GameOverlayRenderPostEvent

        // GuiScreenOpenEvent

        // GuiKeyPressEvent

        // GuiMouseInputEvent

        // BlockOverlayRenderEvent

        // DrawBackgroundEvent

        // GuiActionPerformedEvent

        // InitializeGuiEvent

    }

    private fun postEvent(context: DrawContext, ticks: RenderTickCounter) {
        GameOverlayRenderPreEvent(context, RenderLayer.HOTBAR).post()
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
