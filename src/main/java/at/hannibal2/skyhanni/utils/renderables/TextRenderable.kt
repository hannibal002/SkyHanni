package at.hannibal2.skyhanni.utils.renderables

import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.Text
import net.minecraft.client.Minecraft
import java.awt.Color

open class TextRenderable(
    val text: Text,
    val scale: Double = 1.0,
    val color: Color = Color.WHITE,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
) : Renderable {

    constructor(
        text: String,
        scale: Double = 1.0,
        color: Color = Color.WHITE,
        horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
        verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
    ) : this(Text.of(text), scale, color, horizontalAlign, verticalAlign)

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
