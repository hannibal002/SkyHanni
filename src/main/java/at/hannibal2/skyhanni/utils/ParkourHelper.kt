package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.RenderUtils.outlineTopFace
import at.hannibal2.skyhanni.utils.jsonobjects.ParkourJson
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderWorldLastEvent
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

class ParkourHelper(val locations: List<LorenzVec>, private val shortCuts: List<ParkourJson.ShortCut>) {
    private var current = -1
    private var visible = false

    var rainbowColor = true
    var monochromeColor: Color = Color.WHITE
    var lookAhead = 2
    var outline = true

    fun inParkour() = current != -1

    fun reset() {
        current = -1
        visible = false
    }

    fun render(event: RenderWorldLastEvent) {
        if (current == locations.size - 1) visible = false

        val distanceToPlayer = locations.first().distanceToPlayer()
        if (distanceToPlayer < 2) {
            visible = true
        } else if (distanceToPlayer > 15) {
            if (current < 1) {
                visible = false
            }
        }

        if (!visible) return

        for ((index, location) in locations.withIndex()) {
            if (location.distanceToPlayer() < 2) {
                if (Minecraft.getMinecraft().thePlayer.onGround) {
                    current = index
                }
            }
        }
        if (current < 0) return

        val inProgressVec = getInProgressPair().toSingletonListOrEmpty()
        for ((prev, next) in locations.asSequence().withIndex().zipWithNext().drop(current)
            .take(lookAhead - 1) + inProgressVec) {
            event.draw3DLine(prev.value, next.value, colorForIndex(prev.index), 5, false, colorForIndex(next.index))
        }
        val nextShortcuts = current until current + lookAhead
        for (shortCut in shortCuts) {
            if (shortCut.from in nextShortcuts && shortCut.to in locations.indices) {
                event.draw3DLine(locations[shortCut.from], locations[shortCut.to], Color.RED, 3, false)
                event.drawFilledBoundingBox(axisAlignedBB(locations[shortCut.to]), Color.RED, 1f)
                event.drawDynamicText(locations[shortCut.to].add(-0.5, 1.0, -0.5), "§cShortcut", 2.5)
                if (outline) event.outlineTopFace(axisAlignedBB(locations[shortCut.to]), 2, Color.BLACK, true)
            }
        }

        for ((index, location) in locations.asSequence().withIndex().drop(current)
            .take(lookAhead) + inProgressVec.map { it.second }) {
            var aabb = axisAlignedBB(location)
            if (location !in locations) {
                aabb = aabb.expandBlock()
                event.drawFilledBoundingBox(aabb, colorForIndex(index), .6f)
            } else {
                event.drawFilledBoundingBox(aabb, colorForIndex(index), 1f)
                if (outline) event.outlineTopFace(aabb, 2, Color.BLACK, true)
            }
        }
    }

    private fun getInProgressPair(): Pair<IndexedValue<LorenzVec>, IndexedValue<LorenzVec>>? {
        if (current < 0 || current + lookAhead >= locations.size) return null
        val currentPosition = locations[current]
        val nextPosition = locations[current + 1]
        val lookAheadStart = locations[current + lookAhead - 1]
        val lookAheadEnd = locations[current + lookAhead]

        if (LocationUtils.playerLocation().distance(nextPosition) > currentPosition.distance(nextPosition)) return null

        val factor = LocationUtils.playerLocation().distance(currentPosition) / currentPosition.distance(nextPosition)
        val solpeLocation = lookAheadStart.add(lookAheadEnd.subtract(lookAheadStart).scale(factor))
        return Pair(
            IndexedValue(current + lookAhead - 1, lookAheadStart),
            IndexedValue(current + lookAhead, solpeLocation)
        )
    }

    private fun axisAlignedBB(loc: LorenzVec) = loc.add(-1.0, 0.0, -1.0).boundingToOffset(2, -1, 2).expandBlock()

    private fun colorForIndex(index: Int) = if (rainbowColor) {
        RenderUtils.chromaColor(4.seconds, offset = -index / 12f, brightness = 0.7f)
    } else monochromeColor
}

private fun <T : Any> T?.toSingletonListOrEmpty(): List<T> {
    if (this == null) return emptyList()
    return listOf(this)
}