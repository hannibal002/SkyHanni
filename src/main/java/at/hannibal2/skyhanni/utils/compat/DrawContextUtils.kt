package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f
import org.joml.Quaternionf
import java.nio.FloatBuffer

/**
 * Utils methods related to DrawContext, also known on 1.8 as GLStateManager
 */
object DrawContextUtils {

    private var _drawContext: GuiGraphics? = null

    /**
     * This is used to track the depth of the render context stack.
     * This should usually be 0 or 1 but some rendering events post other rending events
     * and this is used to track the depth of the stack. Sometimes it can reach 2 or 3.
     * This makes sure that we don't clear the context if we are in a nested render event.
     */
    private var renderDepth = 0

    val drawContext: GuiGraphics
        get() = _drawContext ?: run {
            ErrorManager.crashInDevEnv("drawContext is null")
            ErrorManager.skyHanniError("drawContext is null")
        }

    fun drawItem(item: ItemStack, x: Int, y: Int) = drawContext.renderItem(item, x, y)

    fun setContext(context: GuiGraphics) {
        renderDepth++
        if (_drawContext != null) {
            return
        }
        _drawContext = context
    }

    fun clearContext() {
        if (renderDepth == 1) {
            _drawContext = null
            renderDepth = 0
        } else if (renderDepth > 1) {
            renderDepth--
        } else {
            ErrorManager.logErrorStateWithData("Error rendering", "Render depth is negative, something went wrong")
        }
    }

    fun translate(x: Double, y: Double) {
        drawContext.pose().translate(x.toFloat(), y.toFloat())
    }

    fun translate(x: Float, y: Float) {
        drawContext.pose().translate(x, y)
    }

    fun scale(x: Float, y: Float) {
        drawContext.pose().scale(x, y)
    }

    @Deprecated("Use pushPop instead")
    fun pushMatrix() {
        drawContext.pose().pushMatrix()
    }

    @Deprecated("Use pushPop instead")
    fun popMatrix() {
        drawContext.pose().popMatrix()
    }

    /**
     * Push and pop the matrix stack, run the action in between.
     */
    @Suppress("DEPRECATION")
    inline fun pushPop(action: () -> Unit) {
        pushMatrix()
        action()
        popMatrix()
    }

    /**
     * Run operations inside a DrawContext translation
     */
    inline fun translated(x: Number = 0, y: Number = 0, action: () -> Unit) {
        // TODO: when fully modern, use pushPop instead
        translate(x.toFloat(), y.toFloat())
        action()
        translate(-x.toFloat(), -y.toFloat())
    }

    /**
     * Run operations inside a DrawContext scale
     */
    inline fun scaled(x: Number = 1, y: Number = 1, action: () -> Unit) {
        // TODO: when fully modern, use pushPop instead
        scale(x.toFloat(), y.toFloat())
        action()
        scale(1 / x.toFloat(), 1 / y.toFloat())
    }

    fun loadIdentity() {
        drawContext.pose().identity()
    }
}
