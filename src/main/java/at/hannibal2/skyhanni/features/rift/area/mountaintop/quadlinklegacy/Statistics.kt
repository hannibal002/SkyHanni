package at.hannibal2.skyhanni.features.rift.area.mountaintop.quadlinklegacy

internal class Statistics {
    private var exploredNodes: Long = 0
    private var ttHits: Long = 0
    private var ttMisses: Long = 0
    private var ttCacheEvictions: Long = 0
    private var ttSets: Long = 0
    private var ttGets: Long = 0
    private var ttOptimalHits: Long = 0

    private val ttSeenKeys: MutableSet<Long?>? =
        if (Config.STATISTICS_CALCULATE_HIT_RATE_OPTIMAL) HashSet<Long?>() else null

    fun incrementExploredNodes() {
        if (Config.STATISTICS_ENABLED) {
            ++exploredNodes
        }
    }

    fun incrementTTHits() {
        if (Config.STATISTICS_ENABLED) {
            ++ttHits
        }
    }

    fun incrementTTMisses() {
        if (Config.STATISTICS_ENABLED) {
            ++ttMisses
        }
    }

    fun incrementTTCacheEvictions() {
        if (Config.STATISTICS_ENABLED) {
            ++ttCacheEvictions
        }
    }

    fun getExploredNodes(): Long {
        return if (Config.STATISTICS_ENABLED) exploredNodes else 0
    }

    val tTHits: Long
        get() = if (Config.STATISTICS_ENABLED) ttHits else 0

    val tTMisses: Long
        get() = if (Config.STATISTICS_ENABLED) ttMisses else 0

    fun recordTTSet(key: Long) {
        if (Config.STATISTICS_ENABLED) {
            ++ttSets
            if (Config.STATISTICS_CALCULATE_HIT_RATE_OPTIMAL) {
                ttSeenKeys!!.add(key)
            }
        }
    }

    fun recordTTGet(key: Long) {
        if (Config.STATISTICS_ENABLED) {
            ++ttGets
            if (Config.STATISTICS_CALCULATE_HIT_RATE_OPTIMAL && ttSeenKeys!!.contains(key)) {
                ++ttOptimalHits
            }
        }
    }

    val tTCacheEvictions: Long
        get() = if (Config.STATISTICS_ENABLED) ttCacheEvictions else 0

    val tTSets: Long
        get() = if (Config.STATISTICS_ENABLED) ttSets else 0

    val tTGets: Long
        get() = if (Config.STATISTICS_ENABLED) ttGets else 0

    val tTOptimalHits: Long
        get() = if (Config.STATISTICS_ENABLED) ttOptimalHits else 0

    fun reset() {
        if (Config.STATISTICS_ENABLED) {
            exploredNodes = 0
            ttHits = 0
            ttMisses = 0
            ttCacheEvictions = 0
            ttSets = 0
            ttGets = 0
            ttOptimalHits = 0
            if (Config.STATISTICS_CALCULATE_HIT_RATE_OPTIMAL) {
                ttSeenKeys!!.clear()
            }
        }
    }
}
