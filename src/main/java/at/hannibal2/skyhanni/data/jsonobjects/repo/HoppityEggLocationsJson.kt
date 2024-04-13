package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.gson.annotations.Expose

data class HoppityEggLocationsJson(
    @Expose val eggLocations: Map<IslandType, List<LorenzVec>>
)
