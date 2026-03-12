package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RenderUtils.transform
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat

@SkyHanniModule
object MovableHeldItemTooltip {

    private val config get() = SkyHanniMod.feature.gui.heldItemTooltip

    private var post = false

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) {
        if (event.type != RenderLayer.HELD_ITEM_TOOLTIP || !isEnabled()) return
        post = true
        DrawContextUtils.pushMatrix()
        val x = GuiScreenUtils.scaledWindowWidth / 2 - 91
        val y = GuiScreenUtils.scaledWindowHeight - 59
        config.position.transform()
        DrawContextUtils.translate(-x.toFloat(), -y.toFloat())
        GuiEditManager.add(config.position, "Held Item Tooltip", 182 - 1, 10 - 1)
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) {
        if (event.type != RenderLayer.HELD_ITEM_TOOLTIP || !post) return
        DrawContextUtils.popMatrix()
        post = false
    }

    private fun isEnabled() = (SkyBlockUtils.inSkyBlock || (MinecraftCompat.localPlayerExists && config.showOutsideSkyblock)) &&
        config.enabled
}
