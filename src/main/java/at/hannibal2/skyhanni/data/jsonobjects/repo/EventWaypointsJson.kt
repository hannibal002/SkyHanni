package at.hannibal2.skyhanni.data.jsonobjects.repo

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import net.minecraft.world.phys.Vec3

data class EventWaypointsJson(
    @Expose val presents: Map<String, List<EventWaypointData>>,
    @Expose @SerializedName("presents_entrances") val presentsEntrances: Map<String, List<EventWaypointData>>,
)

data class EventWaypointData(
    @Expose val name: String,
    @Expose val position: Vec3,
)
