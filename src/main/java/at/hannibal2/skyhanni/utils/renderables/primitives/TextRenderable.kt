package at.hannibal2.skyhanni.utils.renderables.primitives

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.Text
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils
import net.minecraft.client.Minecraft
import java.awt.Color

// Extension Functions are not inside there Companion Object as it would be ambiguous on import.
// Therefore, they are in the top level and the constructors are just internal not private as they should be.

fun Renderable.Companion.text(
    text: String,
    scale: Double = 1.0,
    color: Color = Color.WHITE,
    horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
) = StringRenderable(text, scale, color, horizontalAlign, verticalAlign)

fun Renderable.Companion.text(
    text: Text,
    scale: Double = 1.0,
    color: Color = Color.WHITE,
    horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
) = TextRenderable(text, scale, color, horizontalAlign, verticalAlign)

class StringRenderable internal constructor(
    val text: String,
    val scale: Double = 1.0,
    val color: Color = Color.WHITE,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
) : Renderable {
    override val width by lazy { (Minecraft.getMinecraft().fontRendererObj.getStringWidth(text) * scale).toInt() + 1 }
    override val height = (9 * scale).toInt() + 1

    private val inverseScale = 1 / scale

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        RenderableUtils.renderString(text, scale, color, inverseScale)
    }

    companion object {
        fun from(text: String) = StringRenderable(text)
    }
}

class TextRenderable internal constructor(
    val text: Text,
    val scale: Double = 1.0,
    val color: Color = Color.WHITE,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
) : Renderable {

    override val width by lazy { (Minecraft.getMinecraft().fontRendererObj.getStringWidth(fixStupid(text)) * scale).toInt() + 1 }
    override val height = (9 * scale).toInt() + 1

    private val inverseScale = 1 / scale

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        RenderableUtils.renderString(fixStupid(text), scale, color, inverseScale)
    }

    //#if MC < 1.21
    private fun fixStupid(text: Text): String {
        return text.text
    }
    //#else
    //$$ private fun fixStupid(text: Text): Text {
    //$$     return text
    //$$ }
    //#endif
}
