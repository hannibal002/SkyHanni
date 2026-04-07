package at.hannibal2.skyhanni.utils.renderables.animated

import at.hannibal2.skyhanni.utils.FakePlayer
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.inPartialSeconds
import at.hannibal2.skyhanni.utils.renderables.FakePlayerRenderable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.animated.framed.EquipmentSlotAnimationState
import net.minecraft.world.entity.EquipmentSlot
import java.awt.Color
import kotlin.time.Duration

/**
 * Draws a [FakePlayer] and advances per-slot [EquipmentSlotAnimationState]s every frame,
 * pushing the correct [net.minecraft.world.item.ItemStack] onto the entity before each draw.
 *
 * When [animatedSlots] is empty the class still functions correctly and simply draws the entity as-is.
 *
 * Obtain instances via [Renderable.Companion.animatedFakePlayer].
 */
class AnimatedFakePlayerRenderable private constructor(
    player: FakePlayer,
    private val animatedSlots: Map<EquipmentSlot, EquipmentSlotAnimationState>,
    followMouse: Boolean,
    eyesX: Float,
    eyesY: Float,
    entityScale: Int,
    padding: Int,
    color: Color?,
    colorCondition: () -> Boolean,
    rawWidth: Int,
    rawHeight: Int,
) : FakePlayerRenderable(player, followMouse, eyesX, eyesY, entityScale, padding, color, colorCondition, rawWidth, rawHeight),
    TimeDependentRenderable {

    override var lastRenderTime: SimpleTimeMark = SimpleTimeMark.now()

    override fun renderWithDelta(mouseOffsetX: Int, mouseOffsetY: Int, deltaTime: Duration) {
        for ((slot, state) in animatedSlots) {
            state.advance(deltaTime.inPartialSeconds)
            player.equipment.set(slot, state.currentStack)
        }
        drawPlayer(mouseOffsetX, mouseOffsetY)
    }

    @Suppress("DEPRECATION")
    @Deprecated("Use renderWithDelta instead", ReplaceWith("renderWithDelta(mouseOffsetX, mouseOffsetY, deltaTime)"))
    override fun render(mouseOffsetX: Int, mouseOffsetY: Int) =
        super<TimeDependentRenderable>.render(mouseOffsetX, mouseOffsetY)

    companion object {
        /**
         * @param animatedSlots maps each equipment slot carrying animated frames to its [EquipmentSlotAnimationState].
         * Slots absent from the map are expected to already be set on [player] and will not be touched per frame.
         * Passing an empty map produces behavior identical to [at.hannibal2.skyhanni.utils.renderables.fakePlayer].
         */
        fun Renderable.Companion.animatedFakePlayer(
            player: FakePlayer,
            animatedSlots: Map<EquipmentSlot, EquipmentSlotAnimationState>,
            followMouse: Boolean = false,
            eyesX: Float = 0f,
            eyesY: Float = 0f,
            width: Int = 50,
            height: Int = 100,
            entityScale: Int = 30,
            padding: Int = 5,
            color: Color? = null,
            colorCondition: () -> Boolean = { true },
        ) = AnimatedFakePlayerRenderable(
            player = player,
            animatedSlots = animatedSlots,
            followMouse = followMouse,
            eyesX = eyesX,
            eyesY = eyesY,
            entityScale = entityScale,
            padding = padding,
            color = color,
            colorCondition = colorCondition,
            rawWidth = width,
            rawHeight = height,
        )
    }
}
