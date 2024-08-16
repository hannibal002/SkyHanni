package at.hannibal2.skyhanni.features.gui.electionviewer

import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe.centerString
import at.hannibal2.skyhanni.features.inventory.wardrobe.CustomWardrobe.config
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ColorUtils.darker
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColorInt
import at.hannibal2.skyhanni.utils.ColorUtils.withAlpha
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

open class ElectionViewer : GuiScreen() {

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        //super.drawScreen(mouseX, mouseY, partialTicks)

        val width = 4 * this.width / 5
        val height = 4 * this.height / 5
        val xTranslate = this.width / 10
        val yTranslate = this.height / 10
        RenderUtils.drawRoundRect(
            xTranslate - 2,
            yTranslate - 2,
            width + 4,
            height + 4,
            Color.BLACK.withAlpha(100),
        )

        GlStateManager.translate(xTranslate.toFloat(), yTranslate.toFloat(), 0f)
        Renderable.withMousePosition(mouseX - xTranslate, mouseY - yTranslate) {
            Renderable.horizontalContainer(
                listOf(
                    createLabeledButton("Current Mayor", Color.CYAN, onClick = { ChatUtils.chat("balls") }),
                    createLabeledButton("Current Election", Color.CYAN, onClick = { }),
                    createLabeledButton("Next Special Mayors", Color.CYAN, onClick = { }),
                ),
                spacing = 10,
                verticalAlign = VerticalAlignment.CENTER,
                horizontalAlign = HorizontalAlignment.CENTER,
            ).renderXYAligned(0, 0, width, height)

        }
        GlStateManager.translate(-xTranslate.toFloat(), -yTranslate.toFloat(), 0f)
    }

    private fun createLabeledButton(
        text: String,
        hoveredColor: Color = Color(130, 130, 130, 200),
        unhoveredColor: Color = hoveredColor.darker(0.57),
        onClick: () -> Unit,
    ): Renderable {
        val buttonWidth = 100
        val buttonHeight = 50

        val renderable = Renderable.hoverable(
            Renderable.drawInsideRoundedRectWithOutline(
                Renderable.doubleLayered(
                    Renderable.clickable(
                        Renderable.placeholder(buttonWidth, buttonHeight),
                        onClick,
                    ),
                    centerString(text),
                    false,
                ),
                hoveredColor,
                padding = 0,
                topOutlineColor = config.color.topBorderColor.toChromaColorInt(),
                bottomOutlineColor = config.color.bottomBorderColor.toChromaColorInt(),
                borderOutlineThickness = 2,
                horizontalAlign = HorizontalAlignment.CENTER,
            ),
            Renderable.drawInsideRoundedRect(
                Renderable.doubleLayered(
                    Renderable.placeholder(buttonWidth, buttonHeight),
                    centerString(text),
                ),
                unhoveredColor.darker(0.57),
                padding = 0,
                horizontalAlign = HorizontalAlignment.CENTER,
            ),
        )

        return renderable
    }

    companion object {
        fun isInGui() = Minecraft.getMinecraft().currentScreen is ElectionViewer
    }
}
