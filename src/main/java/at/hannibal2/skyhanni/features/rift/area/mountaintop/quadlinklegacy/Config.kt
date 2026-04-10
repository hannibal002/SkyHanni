package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy

internal object Config {
    const val TABLE_SIZE_MIB: Int = 64

    const val STATISTICS_ENABLED: Boolean = false
    const val STATISTICS_CALCULATE_HIT_RATE_OPTIMAL: Boolean = false

    init {
        if (Integer.bitCount(TABLE_SIZE_MIB) != 1) {
            throw ExceptionInInitializerError("TABLE_SIZE_MIB must be a power of 2.")
        }

        if (!STATISTICS_ENABLED && STATISTICS_CALCULATE_HIT_RATE_OPTIMAL) {
            throw ExceptionInInitializerError(
                "STATISTICS_CALCULATE_HIT_RATE_OPTIONAL requires STATISTICS_ENABLED."
            )
        }
    }
}
