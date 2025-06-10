package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object RenderEvents {

    init {

        // SkyHanniRenderWorldEvent

        // ScreenDrawnEvent

        // RenderingTickEvent

        // GameOverlayRenderPreEvent

        // GameOverlayRenderPostEvent

        // GuiScreenOpenEvent

        // GuiKeyPressEvent

        // GuiMouseInputEvent

        // BlockOverlayRenderEvent

        // DrawBackgroundEvent

        // GuiActionPerformedEvent

        // InitializeGuiEvent

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
