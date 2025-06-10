package at.hannibal2.skyhanni.config

import io.github.notenoughupdates.moulconfig.common.MyResourceLocation
import io.github.notenoughupdates.moulconfig.common.RenderContext
import io.github.notenoughupdates.moulconfig.gui.GuiOptionEditor

class GuiOptionEditorBlocked(base: GuiOptionEditor) : GuiOptionEditor(base.getOption()) {
    private val base: GuiOptionEditor = base

    override fun render(context: RenderContext, x: Int, y: Int, width: Int) {
        // No super. We delegate and overlay ourselves instead.
        base.render(context, x, y, width)

        // Depress original option
        context.drawColoredRect(x.toFloat(), y.toFloat(), (x + width).toFloat(), (y + height).toFloat(), -0x80000000)

        context.color(1f, 1f, 1f, 1f)
        context.bindTexture(blockedTexture)

        val iconWidth: Float = height * 96f / 64
        context.drawTexturedRect(x.toFloat(), y.toFloat(), iconWidth, height.toFloat())

        val fontRenderer = context.minecraft.defaultFontRenderer
        context.drawStringScaledMaxWidth(
            "This option is currently not available.",
            fontRenderer,
            (x + iconWidth).toInt(), (y + height / 2f - fontRenderer.height / 2f).toInt(),
            true, (width - iconWidth).toInt(), -0xbbbc
        )
        context.color(1f, 1f, 1f, 1f)
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
            "skyhanni", "config_blocked.png"
        )
    }
}
