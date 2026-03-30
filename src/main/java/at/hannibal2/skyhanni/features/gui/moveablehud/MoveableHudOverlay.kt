package at.hannibal2.skyhanni.features.gui.moveablehud

import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.features.gui.moveablehud.MoveableHudConfig
import at.hannibal2.skyhanni.data.GuiEditManager
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPostEvent
import at.hannibal2.skyhanni.events.render.gui.GameOverlayRenderPreEvent
import at.hannibal2.skyhanni.utils.RenderUtils.transform
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.GuiScreenUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat

/**
 * Base for HUD elements that can be repositioned by the user via [GuiEditManager].
 *
 * Push/pop necessarily spans two event handler calls, so [DrawContextUtils.pushMatrix] and
 * [DrawContextUtils.popMatrix] are used directly here — [DrawContextUtils.pushPop] cannot be
 * applied across function boundaries.
 */
abstract class MoveableHudOverlay(
    private vararg val layers: RenderLayer,
    private val displayName: String,
    private val width: Int,
    private val height: Int,
    private val anchorOffsetX: Int,
    private val anchorOffsetY: Int,
) {
    abstract val config: MoveableHudConfig
    private val position: Position get() = config.position
    private fun isEnabled() = config.enabled && inSbEnabled()
    private fun inSbEnabled() = SkyBlockUtils.inSkyBlock || (MinecraftCompat.localPlayerExists && config.showOutsideSkyblock)
    private var matrixPushed = false

    internal open fun onRenderOverlayPre(event: GameOverlayRenderPreEvent) {
        if (event.type !in layers || !isEnabled()) return
        matrixPushed = true
        @Suppress("DEPRECATION")
        DrawContextUtils.pushMatrix()
        val x = GuiScreenUtils.scaledWindowWidth / 2 - anchorOffsetX
        val y = GuiScreenUtils.scaledWindowHeight - anchorOffsetY
        position.transform()
        DrawContextUtils.translate(-x.toFloat(), -y.toFloat()) // Must be after transform to work with scaling
        GuiEditManager.add(position, displayName, width - 1, height - 1) // Editor adds +1
    }

    internal open fun onRenderOverlayPost(event: GameOverlayRenderPostEvent) {
        if (event.type !in layers || !matrixPushed) return
        @Suppress("DEPRECATION")
        DrawContextUtils.popMatrix()
        matrixPushed = false
    }
}
