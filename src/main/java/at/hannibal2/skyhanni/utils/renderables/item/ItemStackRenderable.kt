package at.hannibal2.skyhanni.utils.renderables.item

import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.getTooltipCompat
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.TimeDependentRenderable
import net.minecraft.item.ItemStack
import kotlin.time.Duration

open class ItemStackRenderable(
    private val stackGetter: () -> ItemStack,
    val scale: Double = NeuItems.ITEM_FONT_SIZE,
    val xSpacing: Int = 2,
    ySpacing: Int = 1,
    val rescaleSkulls: Boolean = true,
    override val horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override val verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
) : TimeDependentRenderable() {
    constructor(
        stack: ItemStack,
        scale: Double = NeuItems.ITEM_FONT_SIZE,
        xSpacing: Int = 2,
        ySpacing: Int = 1,
        rescaleSkulls: Boolean = true,
        horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
        verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
    ) : this(
        stackGetter = { stack },
        scale = scale,
        xSpacing = xSpacing,
        ySpacing = ySpacing,
        rescaleSkulls = rescaleSkulls,
        horizontalAlign = horizontalAlign,
        verticalAlign = verticalAlign,
    )

    constructor(
        provider: NeuItemStackProvider,
        scale: Double = NeuItems.ITEM_FONT_SIZE,
        xSpacing: Int = 2,
        ySpacing: Int = 1,
        rescaleSkulls: Boolean = true,
        horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
        verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
    ) : this(
        stackGetter = provider::stack,
        scale = scale,
        xSpacing = xSpacing,
        ySpacing = ySpacing,
        rescaleSkulls = rescaleSkulls,
        horizontalAlign = horizontalAlign,
        verticalAlign = verticalAlign,
    )

    open val stack: ItemStack get() = stackGetter()

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

    fun withTip(advancedTooltipCompat: Boolean = false) = Renderable.hoverTips(
        stack,
        stack.getTooltipCompat(advancedTooltipCompat),
        stack = stack
    )
}
