package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.environment.packetconfig.AbstractPacket

class PositionCommunityFeedback(@JvmField var positions: MutableSet<ComGoalPosition>) : AbstractPacket() {
    class ComGoalPosition(
        @JvmField var goalName: String,
        @JvmField var contribution: Int,
        @JvmField var topPercentage: Double,
        @JvmField var position: Int?
    ) {
        override fun hashCode(): Int {
            return goalName.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ComGoalPosition

            if (contribution != other.contribution) return false
            if (topPercentage != other.topPercentage) return false
            if (position != other.position) return false
            if (goalName != other.goalName) return false

            return true
        }
    }
}
