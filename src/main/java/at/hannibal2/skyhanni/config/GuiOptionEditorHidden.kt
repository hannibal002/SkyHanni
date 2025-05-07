package at.hannibal2.skyhanni.config

import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor

class GuiOptionEditorHidden(base: GuiOptionEditor) : GuiOptionEditor(base.getOption()) {

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        // Don't render anything as we want the option to be hidden from the user
    }

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int): Boolean {
        return false
    }

    override fun keyboardInput(): Boolean {
        return false
    }

    override fun getHeight(): Int {
        // We use -7 to correct the padding between the option above and below the hidden one
        return -7
    }

    override fun fulfillsSearch(word: String?): Boolean {
        return false
    }
}
