package at.hannibal2.hanni.data.jsonobjects.repo

import at.hannibal2.hanni.data.IslandType
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class IslandGraphSettingsJson(
    @Expose @SerializedName("ignored_island_types") val ignoredIslandTypes: Set<IslandType>,
)
