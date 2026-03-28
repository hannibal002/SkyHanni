package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.minecraft.world.phys.Vec3

data class LocationFixJson(
    @Expose val locationFixes: Map<String, LocationFix>,
)

data class LocationFix(
    @Expose val a: Vec3,
    @Expose val b: Vec3,
    @Expose @SerializedName("island_name") val islandName: String,
    @Expose @SerializedName("real_location") val realLocation: String,
)
