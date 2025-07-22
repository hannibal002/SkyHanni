package at.hannibal2.skyhanni.utils.renderables.primitives

import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItemStackProvider
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.getTooltipCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.item.ItemStack

open class ItemStackRenderable protected constructor(
    private val stackGetter: () -> ItemStack,
    val scale: Double = NeuItems.ITEM_FONT_SIZE,
    val xSpacing: Int = 2,
    ySpacing: Int = 1,
    val rescaleSkulls: Boolean = true,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
) : Renderable {

    open val stack: ItemStack get() = stackGetter()

    override val width = (15.5 * scale + 0.5).toInt() + xSpacing
    override val height = (15.5 * scale + 0.5).toInt() + ySpacing

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        stack.renderOnScreen(
            xSpacing / 2f,
            0f,
            scaleMultiplier = scale,
            rescaleSkulls,
        )
    }

    fun withTip(advancedTooltipCompat: Boolean = false) = Renderable.hoverTips(
        stack,
        stack.getTooltipCompat(advancedTooltipCompat),
        stack = stack,
    )

    companion object {
        fun Renderable.Companion.item(
            stackGetter: () -> ItemStack,
            scale: Double = NeuItems.ITEM_FONT_SIZE,
            xSpacing: Int = 2,
            ySpacing: Int = 1,
            rescaleSkulls: Boolean = true,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
        ) = ItemStackRenderable(
            stackGetter = stackGetter,
            scale = scale,
            xSpacing = xSpacing,
            ySpacing = ySpacing,
            rescaleSkulls = rescaleSkulls,
            horizontalAlign = horizontalAlign,
            verticalAlign = verticalAlign,
        )

        fun Renderable.Companion.item(
            stack: ItemStack,
            scale: Double = NeuItems.ITEM_FONT_SIZE,
            xSpacing: Int = 2,
            ySpacing: Int = 1,
            rescaleSkulls: Boolean = true,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
        ) = ItemStackRenderable(
            stackGetter = { stack },
            scale = scale,
            xSpacing = xSpacing,
            ySpacing = ySpacing,
            rescaleSkulls = rescaleSkulls,
            horizontalAlign = horizontalAlign,
            verticalAlign = verticalAlign,
        )

        fun Renderable.Companion.item(
            provider: NeuItemStackProvider,
            scale: Double = NeuItems.ITEM_FONT_SIZE,
            xSpacing: Int = 2,
            ySpacing: Int = 1,
            rescaleSkulls: Boolean = true,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
        ) = ItemStackRenderable(
            stackGetter = provider::stack,
            scale = scale,
            xSpacing = xSpacing,
            ySpacing = ySpacing,
            rescaleSkulls = rescaleSkulls,
            horizontalAlign = horizontalAlign,
            verticalAlign = verticalAlign,
        )

        fun Renderable.Companion.item(
            item: NeuInternalName,
            scale: Double = NeuItems.ITEM_FONT_SIZE,
            xSpacing: Int = 2,
            ySpacing: Int = 1,
            rescaleSkulls: Boolean = true,
            horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
            verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
        ) = item(
            provider = NeuItemStackProvider(item),
            scale = scale,
            xSpacing = xSpacing,
            ySpacing = ySpacing,
            rescaleSkulls = rescaleSkulls,
            horizontalAlign = horizontalAlign,
            verticalAlign = verticalAlign,
        )
    }
}
