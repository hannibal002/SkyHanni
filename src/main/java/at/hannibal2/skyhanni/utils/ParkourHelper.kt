package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils.toSingletonListOrEmpty
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine
import at.hannibal2.skyhanni.utils.RenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.RenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.expandBlock
import at.hannibal2.skyhanni.utils.jsonobjects.ParkourJson
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderWorldLastEvent
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

class ParkourHelper(
    val locations: List<LorenzVec>,
    private val shortCuts: List<ParkourJson.ShortCut>,
    val platformSize: Double = 1.0,
    val detectionRange: Double = 1.0
) {
    private var current = -1
    private var visible = false

    var rainbowColor = false
    var monochromeColor: Color = Color.WHITE
    var lookAhead = 2
    var showEverything = false

    fun inParkour() = current != -1

    fun reset() {
        current = -1
        visible = false
    }

    fun render(event: RenderWorldLastEvent) {
        if (locations.isEmpty()) {
            CopyErrorCommand.logError(
                IllegalArgumentException("locations is empty"),
                "Trying to render an empty parkour"
            )
            return
        }

        try {
            if (!showEverything) {
                if (current == locations.size - 1) visible = false

                val distanceToPlayer = locations.first().offsetCenter().distanceToPlayer()
                if (distanceToPlayer < detectionRange) {
                    visible = true
                } else if (distanceToPlayer > 15) {
                    if (current < 1) {
                        visible = false
                    }
                }

                if (!visible) return

                for ((index, location) in locations.withIndex()) {
                    if (location.offsetCenter().distanceToPlayer() < detectionRange) {
                        if (Minecraft.getMinecraft().thePlayer.onGround) {
                            current = index
                        }
                    }
                }

                if (current < 0) return
            } else {
                current = 0
                lookAhead = locations.size
            }

            val inProgressVec = getInProgressPair().toSingletonListOrEmpty()
            for ((prev, next) in locations.asSequence().withIndex().zipWithNext().drop(current)
                .take(lookAhead - 1) + inProgressVec) {
                event.draw3DLine(
                    prev.value.offsetCenter(),
                    next.value.offsetCenter(),
                    colorForIndex(prev.index),
                    5,
                    false,
                    colorForIndex(next.index)
                )
            }
            val nextShortcuts = current until current + lookAhead
            for (shortCut in shortCuts) {
                if (shortCut.from in nextShortcuts && shortCut.to in locations.indices) {
                    event.draw3DLine(
                        locations[shortCut.from].offsetCenter(),
                        locations[shortCut.to].offsetCenter(),
                        Color.RED,
                        3,
                        false
                    )
                    event.drawFilledBoundingBox(axisAlignedBB(locations[shortCut.to]), Color.RED, 1f)
                    event.drawDynamicText(locations[shortCut.to].add(-0.5, 1.0, -0.5), "§cShortcut", 2.5)
                }

            }

            for ((index, location) in locations.asSequence().withIndex().drop(current)
                .take(lookAhead) + inProgressVec.map { it.second }) {
                val isMovingPlatform = location !in locations
                if (isMovingPlatform && showEverything) continue
                val aabb = if (isMovingPlatform) {
                    axisAlignedBB(location).expandBlock()
                } else axisAlignedBB(location)

                event.drawFilledBoundingBox(aabb, colorForIndex(index), 1f)
                if (!isMovingPlattform) {
                    event.drawString(location.offsetCenter().add(0, 1, 0), "§a§l$index", seeThroughBlocks = true)
                }
            }
        } catch (e: Throwable) {
            CopyErrorCommand.logError(e, "Error while rendering a parkour")
        }
    }

    private fun LorenzVec.offsetCenter() = add(platformSize / 2, 1.0, platformSize / 2)

    private fun getInProgressPair(): Pair<IndexedValue<LorenzVec>, IndexedValue<LorenzVec>>? {
        if (current < 0 || current + lookAhead >= locations.size) return null
        val currentPosition = locations[current].offsetCenter()
        val nextPosition = locations[current + 1].offsetCenter()
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

    private fun axisAlignedBB(loc: LorenzVec) = loc.boundingToOffset(platformSize, 1.0, platformSize).expandBlock()

    private fun colorForIndex(index: Int) = if (rainbowColor) {
        RenderUtils.chromaColor(4.seconds, offset = -index / 12f, brightness = 0.7f)
    } else monochromeColor
}