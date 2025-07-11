package at.hannibal2.skyhanni.utils.renderables.item

import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.EnchantmentsCompat
import at.hannibal2.skyhanni.utils.compat.getTooltipCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.TimeDependentRenderable
import net.minecraft.item.ItemStack
import kotlin.time.Duration

open class ItemStackRenderable(
    stackProvider: AbstractItemStackProvider,
    val scale: Double = NeuItems.ITEM_FONT_SIZE,
    val xSpacing: Int = 2,
    ySpacing: Int = 1,
    val rescaleSkulls: Boolean = true,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
    open val highlight: Boolean = false,
) : TimeDependentRenderable() {
    open val stack: ItemStack = stackProvider.stack.copy().apply {
        if (highlight) addEnchantment(EnchantmentsCompat.PROTECTION.enchantment, 1)
    }

    constructor(
        stack: ItemStack,
        scale: Double = NeuItems.ITEM_FONT_SIZE,
        xSpacing: Int = 2,
        ySpacing: Int = 1,
        rescaleSkulls: Boolean = true,
        horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
        verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
        highlight: Boolean = false,
    ) : this(
        stackProvider = StaticItemStackProvider(stack),
        scale, xSpacing, ySpacing, rescaleSkulls, horizontalAlign, verticalAlign, highlight
    )

    override val width = (15.5 * scale + 0.5).toInt() + xSpacing
    override val height = (15.5 * scale + 0.5).toInt() + ySpacing

    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration) {
        stack.renderOnScreen(
            xSpacing / 2f,
            0f,
            scaleMultiplier = scale,
            rescaleSkulls,
        )
    }

    fun withTip() = Renderable.hoverTips(
        stack,
        stack.getTooltipCompat(false),
        stack = stack
    )
}
