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
    item: ItemStack,
    val scale: Double = NeuItems.ITEM_FONT_SIZE,
    val xSpacing: Int = 2,
    ySpacing: Int = 1,
    val rescaleSkulls: Boolean = true,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
    open val highlight: Boolean = false,
) : TimeDependentRenderable() {
    val stack: ItemStack = item.copy().apply {
        if (highlight) addEnchantment(EnchantmentsCompat.PROTECTION.enchantment, 1)
    }

    override val width = (15.5 * scale + 0.5).toInt() + xSpacing
    override val height = (15.5 * scale + 0.5).toInt() + ySpacing

    override fun renderWithDelta(posX: Int, posY: Int, deltaTime: Duration) {
        stack.renderOnScreen(
            xSpacing / 2.0f,
            0F,
            scaleMultiplier = scale,
            rescaleSkulls,
        )
    }

    fun withTip() = Renderable.hoverTips(stack, stack.getTooltipCompat(false), stack = stack)
}
