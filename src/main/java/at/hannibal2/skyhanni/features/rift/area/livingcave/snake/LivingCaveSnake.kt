package at.hannibal2.skyhanni.features.rift.area.livingcave.snake

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.rift.area.livingcave.LivingCaveSnakeFeatures
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.LineDrawer
import at.hannibal2.skyhanni.utils.RenderUtils.drawColor
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.init.Blocks
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LivingCaveSnake(
    val blocks: MutableList<LorenzVec>,
    private var lastRemoveTime: SimpleTimeMark = SimpleTimeMark.farPast(),
    var lastAddTime: SimpleTimeMark = SimpleTimeMark.farPast(),
    var state: State = State.SPAWNING,
    var lastCalmTime: SimpleTimeMark = SimpleTimeMark.farPast(),
    var lastHitTime: SimpleTimeMark = SimpleTimeMark.farPast(),
    var invalidHeadSince: SimpleTimeMark? = null,
    private var lastBrokenBlock: LorenzVec? = null,
) {
    val head get() = blocks.first()
    private val tail get() = blocks.last()

    fun invalidShape(): Boolean = blocks.isEmpty() || blocks.zipWithNext().any { (a, b) ->
        a.distance(b) > 3
    }

    private fun invalidHeadRightNow(): Boolean = head.getBlockAt() != Blocks.lapis_block

    fun invalidHead(): Boolean = invalidHeadSince?.let { it.passedSince() > 1.seconds } ?: false

    private fun isNotTouchingAir(): Boolean = blocks.any { it.isNotTouchingAir() }

    private fun LorenzVec.isNotTouchingAir(): Boolean = LorenzVec.directions.none { plus(it).getBlockAt() == Blocks.air }

    private fun isSelected() = LivingCaveSnakeFeatures.selectedSnake == this

    fun render(event: SkyHanniRenderWorldEvent, currentRole: LivingCaveSnakeFeatures.Role) {
        if (blocks.isEmpty()) return
        if (LorenzUtils.debug) {
            event.drawString(head.add(0.5, 0.8, 0.5), "§fstate = $state", isSelected())
        }

        val size = blocks.size
        if (size > 1 && state == State.CALM && currentRole == LivingCaveSnakeFeatures.Role.BREAK) {
            val location = lastBrokenBlock?.let {
                LocationUtils.interpolateOverTime(lastRemoveTime, 300.milliseconds, it, tail)
            } ?: tail
            event.renderBlock(location)
        }
        if (currentRole == LivingCaveSnakeFeatures.Role.CALM || size == 1 || state != State.CALM) {
            val location = if (size > 1) {
                LocationUtils.interpolateOverTime(lastAddTime, 200.milliseconds, blocks[1], head)
            } else head
            event.renderBlock(location)
        }
        LineDrawer.draw3D(event.partialTicks) {
            for (block in blocks) {
                if (block == head && lastAddTime.passedSince() < 200.milliseconds) {
                    continue
                }
                drawEdges(block, state.color.toColor(), 2, true)
            }
        }
    }

    private fun SkyHanniRenderWorldEvent.renderBlock(location: LorenzVec) {
        val isSelected = isSelected()
        drawColor(location, state.color.toColor(), alpha = 1f, seeThroughBlocks = isSelected)
        if (isSelected) {
            drawString(location.add(0.5, 0.5, 0.5), state.display, seeThroughBlocks = true)
            drawString(location.add(0.5, 0.2, 0.5), "§b${blocks.size} blocks", seeThroughBlocks = true)
        }
    }

    fun tick() {
        if (invalidHeadRightNow()) {
            if (invalidHeadSince == null) {
                invalidHeadSince = SimpleTimeMark.now()
            }
        } else {
            invalidHeadSince = null
        }
        if (state == State.SPAWNING) return

        state = if (isNotTouchingAir()) {
            State.NOT_TOUCHING_AIR
        } else {
            val notMoving = lastAddTime.passedSince() > 200.milliseconds
            if (notMoving) State.CALM else State.ACTIVE
        }
    }

    fun removeSnakeBlock(location: LorenzVec, lastClickedBlock: LorenzVec?): Boolean {
        // hypixel sends the packet information again when clicking
        if (head == location && location == lastClickedBlock && blocks.size > 1) return false
        blocks.remove(location)
        if (blocks.isEmpty()) {
            return true
        }
        if (state == State.SPAWNING) {
            state = State.ACTIVE
        }
        lastRemoveTime = SimpleTimeMark.now()
        lastBrokenBlock = location

        return false
    }

    enum class State(val color: LorenzColor, label: String) {
        SPAWNING(LorenzColor.AQUA, "Spawning"),
        ACTIVE(LorenzColor.YELLOW, "Active"),
        NOT_TOUCHING_AIR(LorenzColor.RED, "Not touching air"),
        CALM(LorenzColor.GREEN, "Calm"),
        ;

        val display = "${color.getChatColor()}$label snake"
    }
}
