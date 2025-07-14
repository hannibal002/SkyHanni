package at.hannibal2.skyhanni.config

import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor

class GuiOptionEditorBlocked(private val base: GuiOptionEditor, private val extraMessage: String) : GuiOptionEditor(base.getOption()) {

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        // No super. We delegate and overlay ourselves instead.
        base.render(context, x, y, width)

        // Depress original option
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), -0x80000000)

        val iconWidth: Float = height * 96f / 64
        context.drawTexturedRect(blockedTexture, x.toFloat(), y.toFloat(), iconWidth, height.toFloat())

        val fontRenderer = context.minecraft.defaultFontRenderer

        val oneThird: Float = height / 3f

        context.drawStringScaledMaxWidth(
            "This option is currently not available.",
            fontRenderer,
            (x + iconWidth).toInt(), (y + oneThird - fontRenderer.height / 2f).toInt(),
            true, (width - iconWidth).toInt(), -0xbbbc,
        )
        context.drawStringScaledMaxWidth(
            extraMessage,
            fontRenderer,
            (x + iconWidth).toInt(), (y + (oneThird * 2) - fontRenderer.height / 2f).toInt(),
            true, (width - iconWidth).toInt(), -0xbbbc,
        )
    }

    override fun mouseInput(x: Int, y: Int, width: Int, mouseX: Int, mouseY: Int): Boolean {
        return false
    }

    override fun keyboardInput(): Boolean {
        return false
    }

    override fun getHeight(): Int {
        return base.height
    }

    companion object {
        val blockedTexture: MyResourceLocation = MyResourceLocation(
            "skyhanni", "config_blocked.png",
        )
    }
}
