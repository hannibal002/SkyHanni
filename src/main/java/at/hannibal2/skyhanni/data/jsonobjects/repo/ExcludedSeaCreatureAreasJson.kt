package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.data.IslandType
import com.google.gson.annotations.Expose

data class ExcludedSeaCreatureAreasJson(
    @Expose val excludedIslands: Set<IslandType>? = null,
    @Expose val excludedGraphAreas: Set<String>? = null,
)
