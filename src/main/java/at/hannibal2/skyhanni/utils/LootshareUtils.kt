package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawSphereWireframeInWorld
import java.awt.Color

object LootshareUtils {

    const val RANGE = 30.0f

    private val existingCircles = mutableMapOf<LorenzVec, Color>()

    @Suppress("HandleEventInspection")
    fun renderLootshareSphere(event: SkyHanniRenderWorldEvent) {
        var tooCloseSpheres = 0
        for (sphere in existingCircles) {
            if (sphere.key.distance(sphere.key) < 10) tooCloseSpheres++
            if (tooCloseSpheres > 2) continue
            event.drawSphereWireframeInWorld(sphere.value, sphere.key, RANGE)
        }
    }

    fun queuePositionToCircle(position: LorenzVec, color: Color, event: SkyHanniRenderWorldEvent) {
        existingCircles[position] = color
        renderLootshareSphere(event)
    }

    fun isInLootshareRange(pos: LorenzVec): Boolean = pos.distanceToPlayer() < RANGE
}
