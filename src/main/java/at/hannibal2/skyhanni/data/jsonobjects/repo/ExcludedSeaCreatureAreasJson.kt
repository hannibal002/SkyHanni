package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.data.IslandType
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class ExcludedSeaCreatureAreasJson(
    @Expose @SerializedName(value = "excluded_islands", alternate = ["excludedIslands"]) val excludedIslands: Set<IslandType>? = null,
    @Expose @SerializedName(value = "excluded_graph_areas", alternate = ["excludedGraphAreas"]) val excludedGraphAreas: Set<String>? = null,
)
