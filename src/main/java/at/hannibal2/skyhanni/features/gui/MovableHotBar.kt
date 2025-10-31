package at.hannibal2.hanni.features.gui

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.api.minecraftevents.RenderLayer
import at.hannibal2.hanni.data.GuiEditManager
import at.hannibal2.hanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.hanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.RenderUtils.transform
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.compat.DrawContextUtils
import at.hannibal2.hanni.utils.compat.GuiScreenUtils
import at.hannibal2.hanni.utils.compat.MinecraftCompat

@HanniModule
object MovableHotBar {

    private val config get() = HanniMod.feature.gui.hotbar

    private var post = false

    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) {
        if (event.type != RenderLayer.HOTBAR || !isEnabled()) return
        post = true
        DrawContextUtils.pushMatrix()
        val x = GuiScreenUtils.scaledWindowWidth / 2 - 91
        val y = GuiScreenUtils.scaledWindowHeight - 22
        config.hotbar.transform()
        DrawContextUtils.translate(-x.toFloat(), -y.toFloat(), 0f) // Must be after transform to work with scaling
        GuiEditManager.add(config.hotbar, "Hotbar", 182 - 1, 22 - 1) // -1 since the editor for some reason add +1
    }

    @HandleEvent(priority = HandleEvent.HIGHEST)
    fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) {
        if (event.type != RenderLayer.HOTBAR || !post) return
        DrawContextUtils.popMatrix()
        post = false
    }

    fun isEnabled(): Boolean =
        (SkyBlockUtils.inSkyBlock || (MinecraftCompat.localPlayerExists && config.showOutsideSkyblock)) &&
            config.editable
}
