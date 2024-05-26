package at.hannibal2.skyhanni.features.misc.trevor

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.trevor.TrevorMobDiedEvent
import at.hannibal2.skyhanni.features.misc.trevor.TrevorFeatures.onFarmingIsland
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.draw3DLine_nea
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object TalbotTheodoliteSolver {

    var circles = mutableListOf<SkyHanniCircle>()
    var circlePoints = mutableListOf<Pair<LorenzVec, LorenzVec>>()

    var pointsPerCircle = 50

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!onFarmingIsland()) return

        TrevorFeatures.talbotPatternTarget.matchMatcher(event.message.removeColor()) {
            val height = group("height").toInt()
            val above = group("direction") == "above"
            val angle = group("angle").toDouble()

            val heightDiff = (if (above) height else -height).toDouble()

            test(heightDiff, angle)
        }

        if (event.message == "§cYou do not have a trapper's task assigned!") {
            circles.clear()
            update()
        }
        if (event.message.startsWith("§cThis ability is on cooldown for ")) {
            if (InventoryUtils.itemInHandId == "TALBOTS_THEODOLITE".asInternalName()) {
                if (Minecraft.getMinecraft().thePlayer.isSneaking()) {
                    circles.clear()
                    update()
                }
            }
        }
    }

    private fun update() {
        if (circles.size == 4) {
            circles.removeAt(0)
        }

        // make all y the same
        if (circles.size != 0) {
            val first = circles.last()
            val y = first.center.y
            val copy = circles.toList()
            circles.clear()

            for (circle in copy) {
                val center = circle.center
                val radius = circle.radius
                circles.add(SkyHanniCircle(LorenzVec(center.x, y, center.z), radius))
            }
        }

        circlePoints.clear()
        for (circle in circles) {
            val points = createCircle(circle, pointsPerCircle)

            points.zipWithNext { current, next ->
                circlePoints.add(current to next)
            }
            // Connect last point back to the first to close the circle
            circlePoints.add(points.last() to points.first())
        }
    }

    @SubscribeEvent
    fun onTrevorMobDied(event: TrevorMobDiedEvent) {
        circles.clear()
        update()
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        circles.clear()
        update()
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!onFarmingIsland()) return
        if (circles.isEmpty()) return

        for ((a, b) in circlePoints) {
            event.draw3DLine_nea(a, b, Color.RED, 1, true)
        }
    }

    fun createCircle(circle: SkyHanniCircle, amountPoints: Int): List<LorenzVec> {
        val center = circle.center
        val radius = circle.radius
        val list = mutableListOf<LorenzVec>()
        val angleStep = 2 * Math.PI / amountPoints

        for (i in 0 until amountPoints) {
            val angle = i * angleStep
            val x = center.x + radius * cos(angle)
            val y = center.y
            val z = center.z + radius * sin(angle) // Assuming the circle lies in the XZ plane
            list.add(LorenzVec(x, y, z))
        }

        return list
    }

    data class SkyHanniCircle(val center: LorenzVec, val radius: Double, val normal: LorenzVec = LorenzVec(0, 1, 0)) {

        fun intersect(other: SkyHanniCircle): List<LorenzVec> {
            // Ensure circles are in parallel planes (same Y)
            require(center.y == other.center.y) { "Circles must be in parallel planes." }

            // Distance between circle centers in the XZ plane
            val dCenterXZ = center.distance(other.center)

            // Check if circles intersect
            if (dCenterXZ > radius + other.radius) {
                return emptyList() // Circles too far apart
            }
            if (dCenterXZ < abs(radius - other.radius)) {
                return emptyList() // One circle is inside the other
            }

            // Special case: Circles are identical
            if (dCenterXZ == 0.0 && radius == other.radius) {
                return listOf(center) // Infinite intersection points
            }

            // Find the midpoint between the circle centers (in XZ)
            val midpointXZ = LorenzVec(
                (center.x + other.center.x) / 2,
                center.y, // Use the common y-coordinate
                (center.z + other.center.z) / 2
            )

            // Calculate the distance from the midpoint to an intersection point (in XZ)
            val dMidpointToIntersection = sqrt(radius * radius - (dCenterXZ / 2) * (dCenterXZ / 2))

            // Find the unit vector from the first center to the second (in XZ)
            val unitVectorCenterToCenter = other.center.minus(center).normalize()

            // Rotate unit vector by 90 degrees to get a vector perpendicular to it (in XZ)
            val perpendicularUnitVector = LorenzVec(-unitVectorCenterToCenter.z, 0.0, unitVectorCenterToCenter.x)

            // Calculate the two intersection points (in XZ)
            val intersection1XZ = midpointXZ.plus(perpendicularUnitVector.times(dMidpointToIntersection))
            val intersection2XZ = midpointXZ.minus(perpendicularUnitVector.times(dMidpointToIntersection))

            // Add back the y-coordinate to get the final 3D intersection points
            return listOf(intersection1XZ.copy(y = center.y), intersection2XZ.copy(y = center.y))
        }
    }

    infix fun LorenzVec.dot(other: LorenzVec): Double {
        return this.x * other.x + this.y * other.y + this.z * other.z
    }

    fun test(args: Array<String>) {
        val heightDiff = args.getOrNull(0)?.toDouble() ?: return
        val angle = args.getOrNull(1)?.toDouble() ?: return

        test(heightDiff, angle)
    }

    fun calculate3DCircleRadius(heightDifference: Double, angleDegrees: Double): Double {
        val angleRadians = Math.toRadians(angleDegrees)
        return heightDifference / sin(angleRadians)
    }

    private fun test(heightDiff: Double, angleDegrees: Double) {

        val startPoint = LocationUtils.playerLocation()
        val radius = calculate3DCircleRadius(heightDiff, angleDegrees)

        val center = startPoint.add(y = heightDiff)
        val circle = SkyHanniCircle(center, radius)

        circles.add(circle)
        update()
    }
}
