package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer

object LootshareUtils {

    const val RANGE = 30.0f

    data class Sphere(
        val position: LorenzVec,
        var color: LorenzColor = LorenzColor.WHITE,
    )

    fun isInRange(pos: LorenzVec): Boolean = pos.distanceToPlayer() < RANGE
}
