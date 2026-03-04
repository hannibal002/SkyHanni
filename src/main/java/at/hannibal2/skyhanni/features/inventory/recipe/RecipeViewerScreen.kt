package at.hannibal2.skyhanni.features.inventory.recipe

import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.compat.SkyHanniBaseScreen
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.client.gui.screens.Screen
import org.lwjgl.glfw.GLFW

/**
 * A standalone Minecraft [Screen] that hosts the Renderable-based recipe viewer.
 *
 * All recipe logic and Renderable construction lives in [RecipeViewerGui].
 * This class owns mutable UI state and delegates render/input to the Renderable system.
 */
class RecipeViewerScreen(
    internal val internalName: NeuInternalName,
) : SkyHanniBaseScreen() {

    /** Current recipe page, mutated by navigation buttons built in [RecipeViewerGui]. */
    var recipeIndex: Int = 0
        internal set

    private var display: Renderable? = null

    override fun onInitGui() {
        rebuildDisplay()
    }

    /** Rebuilds the display tree; called on init and whenever navigation state changes. */
    internal fun rebuildDisplay() {
        display = RecipeViewerGui.buildDisplay(this)
    }

    override fun onDrawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground(mouseX, mouseY, partialTicks)
        val renderable = display ?: return

        val startX = (width - renderable.width) / 2
        val startY = (height - renderable.height) / 2

        DrawContextUtils.nextStratum()
        DrawContextUtils.pushPop {
            DrawContextUtils.translate(startX.toFloat(), startY.toFloat())
            Renderable.withMousePosition(mouseX - startX, mouseY - startY) {
                renderable.render(0, 0)
            }
        }
    }

    override fun onKeyTyped(typedChar: Char?, keyCode: Int?) {
        val keyCode = keyCode ?: return
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || KeyboardManager.checkIsInventoryClosure(keyCode)) onClose()
    }

    override fun isPauseScreen() = false
}
