package at.hannibal2.skyhanni.features.misc.trevor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawCircleWireframe
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.world.phys.AABB
import java.awt.Color
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.tan

@SkyHanniModule
object TalbotCircles {

    private const val MAX_CONSTRAINTS_USED = 3
    private const val LATTICE_WIDTH = 2
    private const val TOLERANCE = 3.0

    private data class Constraint(val playerPosition: LorenzVec, val dY: Int, val angle: Int)

    private val constraints = mutableListOf<Constraint>()
    private val candidateBlocks = mutableListOf<LorenzVec>()
    private val config get() = SkyHanniMod.feature.misc.trevorTheTrapper

    @Suppress("HandleEventInspection")
    @HandleEvent(onlyOnIsland = IslandType.THE_FARMING_ISLANDS)
    fun drawGuesses(event: SkyHanniRenderWorldEvent) {
        val mobFound = findMob(event)
        if (!(config.talbotCircles && !mobFound)) return
        if (constraints.isEmpty()) return

        if (candidateBlocks.isEmpty()) {
            for (constraint in constraints) {
                val radius = tan(Math.toRadians(90.0 - constraint.angle)) * constraint.dY.absoluteValue
                event.drawCircleWireframe(constraint.playerPosition, radius, Color.ORANGE)
            }
        }

        for (block in candidateBlocks) {
            val blockState = block.getBlockStateAt()
            val blockStateUnder = block.down().getBlockStateAt()
            val isFluid = !blockState.fluidState.isEmpty
            val isAirOverNonAir = blockState.isAir && !blockStateUnder.isAir
            if (isFluid || isAirOverNonAir) {
                val aabb = AABB(block.toBlockPos())
                event.drawFilledBoundingBox(aabb, Color.GREEN, alphaMultiplier = 0.4f, seeThroughBlocks = true)
            }
        }
    }

    fun addResult(dY: Int, angle: Int, playerPosition: LorenzVec) {
        // remove contradicting constraints
        constraints.removeIf {
            (playerPosition.y + dY - it.playerPosition.y - it.dY).absoluteValue > 2 * TOLERANCE
        }

        constraints.add(Constraint(playerPosition, dY, angle))

        // if too many constraints, remove the oldest constraint
        if (constraints.size > MAX_CONSTRAINTS_USED) {
            constraints.removeAt(0)
        }
        recalculateCandidates()
    }
    private fun findMob(event: SkyHanniRenderWorldEvent): Boolean {
        if (!config.solver) return false
        if (TrevorSolver.mobLocation == TrapperMobArea.NONE) return false

        var location = TrevorSolver.mobLocation.coordinates
        if (TrevorSolver.averageHeight != 0.0) {
            location = LorenzVec(location.x, TrevorSolver.averageHeight, location.z)
        }

        val found = TrevorSolver.mobLocation == TrapperMobArea.FOUND
        if (found) {
            val displayName = TrevorSolver.currentMob?.mobName ?: "Mob Location"
            location = TrevorSolver.mobCoordinates
            event.drawWaypointFilled(location.down(2), LorenzColor.GREEN.toColor(), seeThroughBlocks = true, beacon = true)
            event.drawDynamicText(location.up(), displayName, 1.5)
        } else {
            event.drawWaypointFilled(location, LorenzColor.GOLD.toColor(), seeThroughBlocks = true, beacon = true)
            event.drawDynamicText(location.up(), TrevorSolver.mobLocation.location, 1.5)
        }

        return found
    }

    private fun recalculateCandidates() {
        if (constraints.isEmpty()) return

        val yMin = constraints.maxOf { it.playerPosition.y + it.dY - TOLERANCE }
        val yMax = constraints.minOf { it.playerPosition.y + it.dY + TOLERANCE }
        val xMin = constraints.maxOf { it.playerPosition.x - it.maxRadius(yMin, yMax) }
        val xMax = constraints.minOf { it.playerPosition.x + it.maxRadius(yMin, yMax) }
        val zMin = constraints.maxOf { it.playerPosition.z - it.maxRadius(yMin, yMax) }
        val zMax = constraints.minOf { it.playerPosition.z + it.maxRadius(yMin, yMax) }

        candidateBlocks.clear()

        if (yMin > yMax || xMin > xMax || zMin > zMax) return
        if ((xMax - xMin) * (zMax - zMin) > 1_000_000) return

        // using a step size reduces computation and makes it easier for players
        // to judge distance away compared to one big highlighted blob
        for (y in yMin.toInt()..yMax.toInt()) {
            for (x in xMin.toInt()..xMax.toInt() step LATTICE_WIDTH) {
                for (z in zMin.toInt()..zMax.toInt() step LATTICE_WIDTH) {
                    if (constraints.all { it.contains(x, y, z) })
                        candidateBlocks.add(LorenzVec(x, y, z))
                }
            }
        }
    }

    private fun Constraint.maxRadius(yMin: Double, yMax: Double): Double {
        val dyMax = maxOf((yMin - playerPosition.y).absoluteValue, (yMax - playerPosition.y).absoluteValue)
        return tan(Math.toRadians(90.0 - (angle - TOLERANCE).coerceAtLeast(0.1))) * dyMax
    }

    // assumes the height is already within-range, only checks for angle in range
    private fun Constraint.contains(x: Int, y: Int, z: Int): Boolean {
        val dx = (x + 0.5) - playerPosition.x
        val dz = (z + 0.5) - playerPosition.z
        val dist = sqrt(dx * dx + dz * dz)
        val dy = (y.toDouble() - playerPosition.y).absoluteValue

        val candidateAngle = 90 - Math.toDegrees(atan2(dist, dy))
        return (candidateAngle - angle).absoluteValue <= TOLERANCE
    }

    fun resetCircles() {
        constraints.clear()
        candidateBlocks.clear()
    }
}
