package at.hannibal2.skyhanni.utils.renderables.primitives

import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuItemStackProvider
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.compat.getTooltipCompat
import at.hannibal2.skyhanni.utils.renderables.ItemStackProvider
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraft.world.item.ItemStack

open class ItemStackRenderable internal constructor(
    open val config: ItemRenderableConfig,
    private val stackGetter: () -> ItemStack,
) : Renderable {
    val scale: Double get() = config.scale
    val xSpacing: Int get() = config.xSpacing
    val ySpacing: Int get() = config.ySpacing
    val rescaleSkulls get() = config.rescaleSkulls
    override val horizontalAlign: HorizontalAlignment get() = config.horizontalAlign
    override val verticalAlign get() = config.verticalAlign

    open val stack: ItemStack get() = stackGetter()

    override val width: Int get() = (15.5 * scale + 0.5).toInt() + xSpacing
    override val height: Int get() = (15.5 * scale + 0.5).toInt() + ySpacing

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        stack.renderOnScreen(
            xSpacing / 2f,
            ySpacing / 2f,
            scale = scale,
            rescaleSkulls,
        )
    }

    fun withTip(advancedTooltipCompat: Boolean = false) = Renderable.hoverTips(
        stack,
        stack.getTooltipCompat(advancedTooltipCompat),
        stack = stack,
    )

    companion object {
        fun Renderable.Companion.item(stackGetter: () -> ItemStack, config: ItemRenderableConfig.() -> Unit = {}) =
            ItemStackRenderable(ItemRenderableConfig().apply(config), stackGetter)

        fun Renderable.Companion.item(stack: ItemStack, config: ItemRenderableConfig.() -> Unit = {}) =
            item({ stack }, config)

        fun Renderable.Companion.item(provider: ItemStackProvider, config: ItemRenderableConfig.() -> Unit = {}) =
            item(provider::stack, config)

        fun Renderable.Companion.item(item: NeuInternalName, config: ItemRenderableConfig.() -> Unit = {}) =
            item(NeuItemStackProvider(item), config)
    }
}

open class ItemRenderableConfig(
    open var scale: Double = NeuItems.ITEM_FONT_SIZE,
    open var xSpacing: Int = 2,
    open var ySpacing: Int = 1,
    open var rescaleSkulls: Boolean = true,
    open var horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    open var verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
)
