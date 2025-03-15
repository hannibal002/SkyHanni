package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters.registerTypeAdapter
import com.google.gson.Gson
import java.awt.Color

@JvmInline
value class SoopyWaypointList(
    @Expose val waypoints: List<SoopyWaypoint> = listOf(),
) {
    companion object {
        fun fromJson(json: String): SoopyWaypointList = SkyHanniTypeAdapters.SOOPY_WAYPOINT_LIST.fromJson(json)
        fun fromJson(json: JsonElement): SoopyWaypointList = SkyHanniTypeAdapters.SOOPY_WAYPOINT_LIST.fromJsonTree(json)
    }
    fun toJson(): String = SkyHanniTypeAdapters.SOOPY_WAYPOINT_LIST.toJson(this)
}



class SoopyWaypoint(
    val location: LorenzVec,
    val color: Color = Color(0f, 1f, 0f, 0.4f),
    val options: MutableMap<String, String> = mutableMapOf("name" to ""),
)
