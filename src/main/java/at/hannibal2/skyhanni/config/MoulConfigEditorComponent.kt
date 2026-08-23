package at.hannibal2.skyhanni.config

import io.github.notenoughupdates.moulconfig.gui.CloseEventListener
import io.github.notenoughupdates.moulconfig.gui.CloseEventListener.CloseAction
import io.github.notenoughupdates.moulconfig.gui.GuiComponent
import io.github.notenoughupdates.moulconfig.gui.GuiContext
import io.github.notenoughupdates.moulconfig.gui.GuiImmediateContext
import io.github.notenoughupdates.moulconfig.gui.KeyboardEvent
import io.github.notenoughupdates.moulconfig.gui.MoulConfigEditor
import io.github.notenoughupdates.moulconfig.gui.MouseEvent
import io.github.notenoughupdates.moulconfig.internal.Warnings

internal class MoulConfigEditorComponent(
    val editor: MoulConfigEditor<*>,
) : GuiComponent(), CloseEventListener {

    override fun setContext(context: GuiContext) {
        super.setContext(context)
        if (context.root !== this) {
            Warnings.warn("Mounting MoulConfigEditorComponent at location other than root. This can cause issues.")
        }
    }

    override fun getWidth(): Int = mc.scaledWidth

    override fun getHeight(): Int = mc.scaledHeight

    override fun render(context: GuiImmediateContext) {
        if (context.renderOffsetX != 0 || context.renderOffsetY != 0) {
            Warnings.warn("Cannot render MoulConfigEditor with a pretransformed matrix stack")
        }
        editor.render()
    }

    override fun mouseEvent(mouseEvent: MouseEvent, context: GuiImmediateContext): Boolean =
        editor.mouseInput(context.mouseX, context.mouseY, mouseEvent)

    override fun keyboardEvent(event: KeyboardEvent, context: GuiImmediateContext): Boolean =
        editor.keyboardInput(event)

    override fun onBeforeClose(): CloseAction = closeListeners().fold(CloseAction.NO_OBJECTIONS_TO_CLOSE) { action, listener ->
        action.or(listener.onBeforeClose())
    }

    override fun onAfterClose() {
        closeListeners().forEach { it.onAfterClose() }
    }

    private fun closeListeners(): Sequence<CloseEventListener> = sequence {
        yield(editor)
        yieldAll(editor.allOptions.mapNotNull { it.editor as? CloseEventListener })
    }
}
