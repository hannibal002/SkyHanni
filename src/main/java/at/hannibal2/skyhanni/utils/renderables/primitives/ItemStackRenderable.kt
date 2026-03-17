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
import at.hannibal2.skyhanni.utils.system.PropertyVar
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.world.item.ItemStack

open class ItemStackRenderable internal constructor(
    open val config: ItemRenderableConfig,
    private val stackGetter: () -> ItemStack = { ItemStack.EMPTY },
) : Renderable {
    private val scaledSize get() = (15.5 * config.scale + 0.5).toInt()
    override val width: Int get() = scaledSize + config.xSpacing
    override val height: Int get() = scaledSize + config.ySpacing
    override val horizontalAlign get() = config.horizontalAlign
    override val verticalAlign get() = config.verticalAlign

    open val stack: ItemStack get() = stackGetter()
    var stableRenderId: Int? = null
    open fun getStableId() = stableRenderId

    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) {
        this.stableRenderId = stack.renderOnScreen(
            this.config.xSpacing / 2f,
            this.config.ySpacing / 2f,
            this.config,
            stableRenderId = this.stableRenderId,
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

open class ItemRenderableConfig {
    open var scale: Double by PropertyVar(NeuItems.ITEM_FONT_SIZE)
    open var xSpacing: Int by PropertyVar(2)
    open var ySpacing: Int by PropertyVar(1)
    open var rescaleSkulls: Boolean by PropertyVar(true)
    open var horizontalAlign: HorizontalAlignment by PropertyVar(HorizontalAlignment.LEFT)
    open var verticalAlign: VerticalAlignment by PropertyVar { Property.of(VerticalAlignment.CENTER) }
}
