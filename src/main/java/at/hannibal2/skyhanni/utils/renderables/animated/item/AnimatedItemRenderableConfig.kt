package at.hannibal2.skyhanni.utils.renderables.animated.item

import at.hannibal2.skyhanni.utils.NeuItemStackProvider
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.AnimatedBounceStorage
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationStorage
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemRenderableConfig
import net.minecraft.world.item.ItemStack

/**
 * A class that defines behavior for a 'frame' of an ItemStack animation.
 *
 * A ticks parameter of 0 will make the frame last permanently.
 *
 * @param stack The ItemStack that should render during this frame.
 * @param ticks How long this frame should last, in ticks (assuming a nominal 20/s)
 */
class ItemStackAnimationFrame(
    private val stackProvider: () -> ItemStack,
    val ticks: Int = 0,
) {
    constructor(itemStack: ItemStack, ticks: Int = 0) : this({ itemStack }, ticks)
    constructor(provider: NeuItemStackProvider, ticks: Int = 0) : this(provider::stack, ticks)

    val stack: ItemStack get() = stackProvider()
}

class AnimatedItemRenderableConfig(
    val rotationStorage: AnimatedRotationStorage = AnimatedRotationStorage(),
    val bounceStorage: AnimatedBounceStorage = AnimatedBounceStorage(),
    override var scale: Double = NeuItems.ITEM_FONT_SIZE,
    override var xSpacing: Int = 2,
    override var ySpacing: Int = 1,
    override var rescaleSkulls: Boolean = true,
    override var horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override var verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
) : ItemRenderableConfig(scale, xSpacing, ySpacing, rescaleSkulls, horizontalAlign, verticalAlign)
