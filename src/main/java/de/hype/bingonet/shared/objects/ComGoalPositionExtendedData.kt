package de.hype.bingonet.shared.objects

import de.hype.bingonet.shared.packets.function.PositionCommunityFeedback.ComGoalPosition
import java.time.Instant
import java.time.temporal.ChronoUnit

class ComGoalPositionExtendedData(
    @JvmField val position: ComGoalPosition,
    val mcuuid: java.util.UUID,
    @JvmField val fromTime: Instant
) {
    val referenceTime: Instant?
        get() {
            val position = position.position ?: return null
            val punPos = (100 - position)
            return fromTime.plus(punPos.toLong() * punPos, ChronoUnit.MINUTES)
        }

    fun dataEquals(that: ComGoalPositionExtendedData?): Boolean {
        if (that == null) return false
        return mcuuid == that.mcuuid && position == that.position
    }
}
