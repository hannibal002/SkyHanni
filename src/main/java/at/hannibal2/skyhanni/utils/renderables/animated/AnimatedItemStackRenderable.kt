package at.hannibal2.skyhanni.utils.renderables.animated

import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.GuiRenderUtils.renderOnScreen
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.RenderUtils.HorizontalAlignment
import at.hannibal2.skyhanni.utils.RenderUtils.VerticalAlignment
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.AnimatedBounceLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.AnimatedBounceStorage
import at.hannibal2.skyhanni.utils.renderables.animated.bounce.BouncingBehavior
import at.hannibal2.skyhanni.utils.renderables.animated.framed.AnimatedFrameLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.framed.AnimatedFrameStorage
import at.hannibal2.skyhanni.utils.renderables.animated.framed.FramedBehavior
import at.hannibal2.skyhanni.utils.renderables.animated.framed.ItemStackAnimatedFrame
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationLocalStorage
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.AnimatedRotationStorage
import at.hannibal2.skyhanni.utils.renderables.animated.rotate.RotatingBehavior
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemRenderableConfig
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable
import net.minecraft.world.item.ItemStack
import kotlin.time.Duration

class AnimatedItemStackRenderable private constructor(
    override val config: AnimatedItemRenderableConfig,
) : ItemStackRenderable(config, {
    config.frameStorage.frames.firstOrNull()?.stack ?: ErrorManager.skyHanniError(
        "Cannot initialize AnimatedItemStackRenderable with an empty animation context."
    )
}),
    TimeDependentRenderable,
    BouncingBehavior,
    RotatingBehavior,
    FramedBehavior<ItemStackAnimatedFrame> {
    override val stack: ItemStack get() = currentFrame.stack
    override val bounceStorage: AnimatedBounceStorage get() = config.bounceStorage
    override val rotationStorage: AnimatedRotationStorage get() = config.rotationStorage
    override val frameStorage: AnimatedFrameStorage<ItemStackAnimatedFrame> get() = config.frameStorage

    override val bounceStartTime: SimpleTimeMark = SimpleTimeMark.now()
    override var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()
    override var ticksInFrame: Double = 0.0

    private var stableRenderId: Int = -1
    fun getStableId() = stableRenderId

    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration) {
        applyRotation(deltaTime)
        applyBounce()
        tryMoveNextFrame(deltaTime.inPartialSeconds)

        this.stableRenderId = stack.renderOnScreen(
            x = (xSpacing / 2f),
            y = 0f,
            scale = config.scale,
            rescaleSkulls = rescaleSkulls,
            rotationVec = currentRotation,
            translationVec = currentBounce,
            stableRenderId = this.stableRenderId,
        )
    }

    @Suppress("DEPRECATION")
    @Deprecated("Use renderWithDelta instead", ReplaceWith("renderWithDelta(posX, posY, deltaTime)"))
    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) = super<TimeDependentRenderable>.render(mouseOffsetX, mouseOffsetY)

    companion object {
        fun Renderable.Companion.animatedItemStack(
            config: AnimatedItemRenderableConfig.() -> Unit = {
                AnimatedItemRenderableConfig()
            },
        ) = AnimatedItemStackRenderable(AnimatedItemRenderableConfig().apply(config))
    }
}

class AnimatedItemRenderableConfig(
    var frameStorage: AnimatedFrameStorage<ItemStackAnimatedFrame> = AnimatedFrameLocalStorage(emptyList()),
    var rotationStorage: AnimatedRotationStorage = AnimatedRotationLocalStorage(),
    var bounceStorage: AnimatedBounceStorage = AnimatedBounceLocalStorage(),
    override var scale: Double = NeuItems.ITEM_FONT_SIZE,
    override var xSpacing: Int = 2,
    override var ySpacing: Int = 1,
    override var rescaleSkulls: Boolean = true,
    override var horizontalAlign: HorizontalAlignment = HorizontalAlignment.LEFT,
    override var verticalAlign: VerticalAlignment = VerticalAlignment.CENTER,
) : ItemRenderableConfig(scale, xSpacing, ySpacing, rescaleSkulls, horizontalAlign, verticalAlign)
