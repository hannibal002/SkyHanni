package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose

data class HoppityEggLocationsJson(
    @Expose val eggLocations: Map<IslandType, List<LorenzVec>>,
    @Expose val rabbitSlots: Map<Int, Int>,
    @Expose val otherUpgradeSlots: Set<Int>,
    @Expose val noPickblockSlots: Set<Int>,
    @Expose val barnIndex: Int,
    @Expose val infoIndex: Int,
    @Expose val productionInfoIndex: Int,
    @Expose val prestigeIndex: Int,
    @Expose val milestoneIndex: Int,
    @Expose val leaderboardIndex: Int,
    @Expose val maxRabbits: Int,
)
