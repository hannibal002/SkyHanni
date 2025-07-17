package at.hannibal2.skyhanni.utils.renderables.primitives

import at.hannibal2.skyhanni.utils.GuiRenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.StringUtils.splitLinesWithLength
import at.hannibal2.skyhanni.utils.compat.DrawContextUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import java.awt.Color
import kotlin.math.ceil

class WrappedStringRenderable private constructor(
    text: String,
    setWidth: Int,
    val scale: Double = 1.0,
    val color: Color = Color.WHITE,
    override val horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
    override val verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
    private val internalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
) : Renderable {

    private val fontRenderer: FontRenderer by lazy { Minecraft.getMinecraft().fontRendererObj }
    val map: List<Pair<String, Int>> by lazy {
        splitLinesWithLength(text, ceil(setWidth / scale).toInt() - 1, fontRenderer).first
    }

    override val width by lazy { (rawWidth * scale).toInt() + 1 }

    private val rawWidth by lazy {
        if (map.size == 1) map.first().second
        else map.maxOf { it.second }
    }

    override val height by lazy { map.size * ((9 * scale).toInt() + 1) }

    private val inverseScale = 1 / scale

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

    companion object {
        fun Renderable.Companion.wrappedText(
            text: String,
            setWidth: Int,
            scale: Double = 1.0,
            color: Color = Color.WHITE,
            horizontalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
            verticalAlign: RenderUtils.VerticalAlignment = RenderUtils.VerticalAlignment.CENTER,
            internalAlign: RenderUtils.HorizontalAlignment = RenderUtils.HorizontalAlignment.LEFT,
        ) = WrappedStringRenderable(text, setWidth, scale, color, horizontalAlign, verticalAlign, internalAlign)
    }
}
