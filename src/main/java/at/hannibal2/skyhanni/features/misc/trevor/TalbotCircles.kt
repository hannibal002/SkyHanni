package at.hannibal2.skyhanni.features.misc.trevor

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawCircleWireframe
import java.awt.Color
import kotlin.math.absoluteValue
import kotlin.math.tan

object TalbotCircles {

    private const val MAXIMUM_RADIUS = 500.0

    private data class Circle(val center: LorenzVec, val radius: Double)

    private val circles = mutableListOf<Circle>()

    fun drawCircles(event: SkyHanniRenderWorldEvent) {
        for (circle in circles) {
            event.drawCircleWireframe(circle.center, circle.radius, Color.ORANGE)
        }
    }

    fun addResult(dY: Int, angle: Int) {
        val radius = tan(Math.toRadians(90.0 - angle)) * dY.absoluteValue
        if (radius in 0.0..MAXIMUM_RADIUS) {
            val playerPosition = LocationUtils.playerLocation().roundTo(2)
            val center = LorenzVec(playerPosition.x, playerPosition.y + dY, playerPosition.z)
            circles.add(Circle(center, radius))
        }
    }

    fun resetCircles() {
        circles.clear()
    }
}
