package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.transform
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraftforge.client.event.RenderGameOverlayEvent

@SkyHanniModule
object MovableXPBar {

    private val config get() = SkyHanniMod.feature.gui.xpBar

    private var post = false

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) {
        if (event.type != RenderGameOverlayEvent.ElementType.EXPERIENCE || !isEnabled()) return
        post = true
        GlStateManager.pushMatrix()
        val scaled = event.resolution
        val x = scaled.scaledWidth / 2 - 91
        val y = scaled.scaledHeight - 29
        config.position.transform()
        GlStateManager.translate(-x.toFloat(), -y.toFloat(), 0f) // Must be after transform to work with scaling
        GuiEditManager.add(config.position, "XP Bar", 182 - 1, 5 - 1) // -1 since the editor for some reason add +1
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) {
        if (event.type != RenderGameOverlayEvent.ElementType.EXPERIENCE || !post) return
        GlStateManager.popMatrix()
        post = false
    }

    private fun isEnabled() = (LorenzUtils.inSkyBlock || (Minecraft.getMinecraft().thePlayer != null && config.showOutsideSkyblock)) &&
        config.enabled
}
