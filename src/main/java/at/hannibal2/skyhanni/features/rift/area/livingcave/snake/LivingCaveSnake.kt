package at.hannibal2.skyhanni.features.rift.area.livingcave.snake

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.rift.RiftApi
import at.hannibal2.skyhanni.features.rift.area.livingcave.LivingCaveSnakeFeatures
import at.hannibal2.skyhanni.features.rift.area.livingcave.LivingCaveSnakeFeatures.FROZEN_WATER_PUNGI
import at.hannibal2.skyhanni.features.rift.area.livingcave.LivingCaveSnakeFeatures.pickaxes
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RecalculatingValue
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.render.LineDrawer
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawColor
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import io.github.notenoughupdates.moulconfig.ChromaColour
import net.minecraft.world.level.block.Blocks
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
    private val config get() = RiftApi.config.area.livingCave.snakeHelper
    private val hasBothItems by RecalculatingValue(1.seconds) {
        if (!IslandType.THE_RIFT.isInIsland()) return@RecalculatingValue false
        val hotbar = InventoryUtils.getItemsInHotbar()

        val hasBreak = hotbar.any { it.getInternalName() in pickaxes }
        val hasPungi = hotbar.any { it.getInternalName() == FROZEN_WATER_PUNGI }

        hasBreak && hasPungi
    }

    fun invalidShape(): Boolean = blocks.isEmpty() || blocks.zipWithNext().any { (a, b) ->
        a.distance(b) > 3
    }

    private fun invalidHeadRightNow(): Boolean = head.getBlockAt() != Blocks.LAPIS_BLOCK

    fun invalidHead(): Boolean = invalidHeadSince?.let { it.passedSince() > 1.seconds } ?: false

    private fun isNotTouchingAir(): Boolean = blocks.any { it.isNotTouchingAir() }

    private fun LorenzVec.isNotTouchingAir(): Boolean = LorenzVec.directions.none { plus(it).getBlockAt() == Blocks.AIR }

    private fun isSelected() = LivingCaveSnakeFeatures.selectedSnake == this

    fun render(event: SkyHanniRenderWorldEvent, currentRole: LivingCaveSnakeFeatures.Role) {
        if (blocks.isEmpty()) return
        if (SkyBlockUtils.debug) {
            event.drawString(head.add(0.5, 0.8, 0.5), "§fstate = $state", isSelected())
        }

        val size = blocks.size
        if (config.solo && hasBothItems) {
            if (size > 1) {
                val tailLocation = lastBrokenBlock?.let {
                    LocationUtils.interpolateOverTime(lastRemoveTime, 300.milliseconds, it, tail)
                } ?: tail
                event.renderBlock(tailLocation, LorenzColor.DARK_BLUE.toChromaColor())
            }
        } else {
            if (size > 1 && state == State.CALM && currentRole == LivingCaveSnakeFeatures.Role.BREAK) {
                val location = lastBrokenBlock?.let {
                    LocationUtils.interpolateOverTime(lastRemoveTime, 300.milliseconds, it, tail)
                } ?: tail
                event.renderBlock(location, state.chromaColor)
            }
            if (currentRole == LivingCaveSnakeFeatures.Role.CALM || size == 1 || state != State.CALM) {
                val location = if (size > 1) {
                    LocationUtils.interpolateOverTime(lastAddTime, 200.milliseconds, blocks[1], head)
                } else head
                event.renderBlock(location, state.chromaColor)
            }
        }

        val headLocation = if (size > 1) {
            LocationUtils.interpolateOverTime(lastAddTime, 200.milliseconds, blocks[1], head)
        } else {
            head
        }
        event.renderBlock(headLocation, state.chromaColor)

        LineDrawer.draw3D(event, lineWidth = 2, depth = true) {
            for (block in blocks) {
                if (block == head && lastAddTime.passedSince() < 200.milliseconds) {
                    continue
                }
                drawEdges(block, state.chromaColor.toColor())
            }
        }
    }

    private fun SkyHanniRenderWorldEvent.renderBlock(location: LorenzVec, color: ChromaColour) {
        val isSelected = isSelected()
        drawColor(location, color, alpha = 1f, seeThroughBlocks = isSelected)
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

    enum class State(color: LorenzColor, label: String) {
        SPAWNING(LorenzColor.AQUA, "Spawning"),
        ACTIVE(LorenzColor.YELLOW, "Active"),
        NOT_TOUCHING_AIR(LorenzColor.RED, "Not touching air"),
        CALM(LorenzColor.GREEN, "Calm"),
        ;

        val display = "${color.getChatColor()}$label snake"

        val chromaColor = color.toChromaColor()
    }
}
