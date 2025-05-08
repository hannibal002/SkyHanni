package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class IslandTypeJson(
    @Expose val islands: Map<String, IslandJson>,
    @Expose @SerializedName("max_players") val maxPlayers: Int,
    @Expose @SerializedName("max_players_mega") val maxPlayersMega: Int,
)

data class IslandJson(
    @Expose val name: String,
    @Expose @SerializedName("api_name") val apiName: String? = null,
    @Expose @SerializedName("max_players") val maxPlayers: Int? = null,
    @Expose val bounds: IslandBounds? = null
)

data class IslandBounds(
    @Expose @SerializedName("max_x") val maxX: Int,
    @Expose @SerializedName("min_x") val minX: Int,
    @Expose @SerializedName("max_z") val maxZ: Int,
    @Expose @SerializedName("min_z") val minZ: Int
)
