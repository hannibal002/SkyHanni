package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.StringUtils.splitLines
import at.hannibal2.skyhanni.utils.StringUtils.splitLinesWithLength
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import java.awt.Color
import kotlin.math.ceil

open class StringRenderable(
    val text: String,
    val scale: Double = 1.0,
    val color: Color = Color.WHITE,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
) : Renderable {
    override val width by lazy { (Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) * scale).toInt() + 1 }
    override val height = (9 * scale).toInt() + 1

    val inverseScale = 1 / scale

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        RenderableUtils.renderString(text, scale, color, inverseScale)
    }

    companion object {
        fun from(text: String) = StringRenderable(text)
    }
}

class WrappedStringRenderable(
    text: String,
    width: Int,
    scale: Double = 1.0,
    color: Color = Color.WHITE,
    horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
    private val internalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
) : Renderable, StringRenderable(
    text,
    scale,
    color,
    horizontalAlign,
    verticalAlign,
) {
    private val fontRenderer: FontRenderer by lazy { Minecraft.getMinecraft().fontRendererObj }
    val map: List<Pair<String, Int>> by lazy {
        splitLinesWithLength(text, ceil(width / scale).toInt() - 1, fontRenderer).first
    }

    override val width by lazy { (rawWidth * scale).toInt() + 1 }

    private val rawWidth by lazy {
        if (map.size == 1) map.first().second
        else map.maxOf { it.second }
    }

    override val height by lazy { map.size * ((9 * scale).toInt() + 1) }

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        DrawContextUtils.translate(1.0, 1.0, 0.0)
        DrawContextUtils.scale(scale.toFloat(), scale.toFloat(), 1f)
        map.forEachIndexed { index, (text, size) ->
            GuiRenderUtils.drawString(
                text,
                RenderableUtils.calculateAlignmentXOffset(size, rawWidth, internalAlign).toFloat(),
                index * 10f,
                color.rgb,
            )
        }
        DrawContextUtils.scale(inverseScale.toFloat(), inverseScale.toFloat(), 1f)
        DrawContextUtils.translate(-1.0, -1.0, 0.0)
    }
}
