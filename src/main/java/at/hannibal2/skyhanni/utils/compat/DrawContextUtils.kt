package at.hannibal2.skyhanni.utils.compat

import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.util.Vec3
//#if MC > 1.21
//$$ import net.minecraft.client.gui.DrawContext
//#endif

/**
 * Utils methods related to DrawContext, also known on 1.8 as GLStateManager
 */
object DrawContextUtils {

    private var _drawContext: DrawContext? = null

    /**
     * This is used to track the depth of the render context stack.
     * This should usually be 0 or 1 but some rendering events post other rending events
     * and this is used to track the depth of the stack. Sometimes it can reach 2 or 3.
     * This makes sure that we don't clear the context if we are in a nested render event.
     */
    private var renderDepth = 0

    val drawContext: DrawContext
        get() = _drawContext ?: run {
            ErrorManager.crashInDevEnv("drawContext is null")
            ErrorManager.skyHanniError("drawContext is null")
        }

    fun setContext(context: DrawContext) {
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
            ErrorManager.skyHanniError("Render depth is negative, something went wrong")
        }
    }

    fun translate(x: Double, y: Double, z: Double) {
        drawContext.matrices.translate(x, y, z)
    }

    fun translate(x: Float, y: Float, z: Float) {
        drawContext.matrices.translate(x, y, z)
    }

    fun translate(vec: Vec3) {
        drawContext.matrices.translate(vec)
    }

    fun scale(x: Float, y: Float, z: Float) {
        drawContext.matrices.scale(x, y, z)
    }

    fun pushMatrix() {
        drawContext.matrices.pushMatrix()
    }

    fun popMatrix() {
        drawContext.matrices.popMatrix()
    }
}
