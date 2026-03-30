package at.hannibal2.skyhanni.api.minecraftevents

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.RenderData
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.render.SkyHanniRoundedShapeRenderManager
import at.hannibal2.skyhanni.utils.render.item.SkyHanniItemRenderCoordinator
import at.hannibal2.skyhanni.utils.render.item.SkyHanniPipCoordinatorRenderer
//~ if > 1.21.11 'SpecialGuiElementRegistry' -> 'PictureInPictureRendererRegistry'
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
//~ if > 1.21.11 '.v1.world.WorldRenderContext' -> '.v1.level.LevelRenderContext'
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
//~if > 1.21.11 '.v1.world.WorldRenderEvents' -> '.v1.level.LevelRenderEvents'
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.minecraft.client.DeltaTracker
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.resources.Identifier
//? if > 1.21.11
//import at.hannibal2.skyhanni.utils.compat.getRenderState

@SkyHanniModule
object RenderEvents {

    init {
        HudElementRegistry.attachElementBefore(
            VanillaHudElements.SLEEP,
            Identifier.fromNamespaceAndPath("skyhanni", "gui_render_layer"),
            RenderEvents::postGui
        )

        //~ if > 1.21.11 'SpecialGuiElementRegistry' -> 'PictureInPictureRendererRegistry'
        SpecialGuiElementRegistry.register { ctx ->
            SkyHanniPipCoordinatorRenderer(
                //~ if > 1.21.11 'vertexConsumers' -> 'bufferSource'
                ctx.vertexConsumers()
            )
        }

        // makes the lines render weird idk
        //~ if > 1.21.11 'WorldRenderEvents' -> 'LevelRenderEvents'
        //~ if > 1.21.11 'WorldRenderContext' -> 'LevelRenderContext'
        WorldRenderEvents.END_MAIN.register { event: WorldRenderContext ->
            //~ if > 1.21.11 '.consumers() as? MultiBufferSource.BufferSource ?: return@register' -> '.bufferSource()'
            val immediateVertexConsumers = event.consumers() as? MultiBufferSource.BufferSource ?: return@register
            //~ if > 1.21.11 '.matrices()' -> '.poseStack()'
            val stack = event.matrices()
            SkyHanniRenderWorldEvent(
                stack,
                //~ if > 1.21.11 'mainCamera' -> 'mainCamera.getRenderState()'
                event.gameRenderer().mainCamera,
                immediateVertexConsumers,
                Minecraft.getInstance().deltaTracker.realtimeDeltaTicks,
            ).post()
        }
    }

    @HandleEvent
    fun onResourcePackReload() {
        SkyHanniItemRenderCoordinator.invalidateAtlas()
        SkyHanniRoundedShapeRenderManager.invalidateAtlas()
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

    @JvmStatic
    fun postHeldItemTooltipLayerEventPre(context: GuiGraphics): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.HELD_ITEM_TOOLTIP).post()
    }

    @JvmStatic
    fun postHeldItemTooltipLayerEventPost(context: GuiGraphics) {
        GameOverlayRenderPostEvent(context, RenderLayer.HELD_ITEM_TOOLTIP).post()
    }

    @JvmStatic
    fun postActionBarLayerEventPre(context: GuiGraphics): Boolean {
        return GameOverlayRenderPreEvent(context, RenderLayer.ACTION_BAR).post()
    }

    @JvmStatic
    fun postActionBarLayerEventPost(context: GuiGraphics) {
        GameOverlayRenderPostEvent(context, RenderLayer.ACTION_BAR).post()
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
    HELD_ITEM_TOOLTIP,
    ACTION_BAR,

    // Not a real forge layer but is used on modern Minecraft versions
    EXPERIENCE_NUMBER,
}
