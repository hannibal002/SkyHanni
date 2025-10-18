package at.hannibal2.skyhanni.utils.system

data class MCVersion(val epoch: Int, val major: Int, val minor: Int) : Comparable<MCVersion> {

    inline val asString get() = toString()

    override fun toString(): String = "$epoch.$major.$minor"

    override fun compareTo(other: MCVersion): Int {
        return when {
            epoch != other.epoch -> epoch.compareTo(other.epoch)
            major != other.major -> major.compareTo(other.major)
            else -> minor.compareTo(other.minor)
        }
    }

    /** Inclusive for both borders */
    fun isInBetween(current: MCVersion, target: MCVersion): Boolean {
        if (this > target) return false
        if (this < current) return false
        if (this == current) return true
        return true
    }

    companion object {
        fun fromString(version: String): MCVersion {
            val parts = version.split('.')
            return MCVersion(
                parts.getOrNull(0)?.toIntOrNull() ?: 0,
                parts.getOrNull(1)?.toIntOrNull() ?: 0,
                parts.getOrNull(2)?.toIntOrNull() ?: 0,
            )
        }
    }
}
