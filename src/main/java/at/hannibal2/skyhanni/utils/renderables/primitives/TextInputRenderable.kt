package at.hannibal2.skyhanni.utils.renderables.primitives

import at.hannibal2.skyhanni.data.model.TextInput
import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import java.awt.Color

/**
 * Creates an interactive text-input Renderable backed by a [TextInput].
 *
 * @param textInput The [TextInput] instance that holds cursor and text state.
 * @param isActive A supplier returning true when this field currently has focus.
 * @param onActivate Called when the user clicks the field to request focus.
 * @param width The fixed pixel width of the input field.
 */
fun Renderable.Companion.textInput(
    textInput: TextInput,
    isActive: () -> Boolean,
    onActivate: () -> Unit,
    width: Int,
): Renderable = clickable(
    object : Renderable {
        override val width = width
        override val height = 16
        override val horizontalAlign = HorizontalAlignment.LEFT
        override val verticalAlign = VerticalAlignment.TOP

        override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
            val active = isActive()
            if (active) {
                GuiRenderUtils.drawFloatingRectLight(0, 0, width, height, false)
                textInput.makeActive()
            } else {
                GuiRenderUtils.drawFloatingRectDark(0, 0, width, height, false)
            }
            val displayText = if (active) textInput.editText() else textInput.textBox
            DrawContextUtils.pushPop {
                DrawContextUtils.translate(3f, ((height - 8) / 2).toFloat())
                RenderableUtils.renderString(displayText, scale = 1.0, color = Color.WHITE)
            }
        }
    },
    onLeftClick = onActivate,
    bypassChecks = true,
)
