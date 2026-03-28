package at.hannibal2.skyhanni.features.misc.trevor

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.VectorUtils.roundTo
import at.hannibal2.skyhanni.utils.VectorUtils.up
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawCircleWireframe
import net.minecraft.world.phys.Vec3
import java.awt.Color
import kotlin.math.absoluteValue
import kotlin.math.tan

object TalbotCircles {

    private const val MAXIMUM_RADIUS = 500.0

    private data class Circle(val center: Vec3, val radius: Double)

    private val circles = mutableListOf<Circle>()

    fun drawCircles(event: SkyHanniRenderWorldEvent) {
        for (circle in circles) {
            event.drawCircleWireframe(circle.center, circle.radius, Color.ORANGE)
        }
    }

    fun addResult(dY: Double, angle: Int) {
        val radius = tan(Math.toRadians(90.0 - angle)) * dY.absoluteValue
        if (radius in 0.0..MAXIMUM_RADIUS) {
            val center = LocationUtils.playerLocation().up(dY).roundTo(2)
            circles.add(Circle(center, radius))
        }
    }

    fun resetCircles() {
        circles.clear()
    }
}
