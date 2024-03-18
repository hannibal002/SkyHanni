package at.hannibal2.skyhanni.features.garden.farming.lane

import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.utils.LorenzVec

object FarmingLaneAPI {

    val lanes = mutableMapOf<CropType, FarmingLane>()

    fun FarmingDirection.getValue(location: LorenzVec): Double = when (this) {
        FarmingDirection.NORTH_SOUTH -> location.z
        FarmingDirection.OST_WEST -> location.x
    }

    fun FarmingDirection.setValue(location: LorenzVec, value: Double): LorenzVec = when (this) {
        FarmingDirection.NORTH_SOUTH -> location.copy(z = value)
        FarmingDirection.OST_WEST -> location.copy(x = value)
    }
}
