package at.hannibal2.skyhanni.data.jsonobjects.repo

import at.hannibal2.skyhanni.data.IslandType
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class WarpsJson(
    @Expose val warpCommands: List<String>,
    @Expose val warpLocation: Map<String, WarpLocationData>,
)

data class WarpLocationData(
    @Expose @SerializedName("display_name") val displayName: String,
    @Expose val island: IslandType,
    @Expose val x: Double,
    @Expose val y: Double,
    @Expose val z: Double,
    @Expose @SerializedName("extra_diana_warp_blocks") val extraDianaWarpBlocks: Int,
)
