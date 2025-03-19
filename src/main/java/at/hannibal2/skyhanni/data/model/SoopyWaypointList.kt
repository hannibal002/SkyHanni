package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import java.awt.Color

@JvmInline
value class SoopyWaypointList(
    @Expose val waypoints: MutableList<SoopyWaypoint> = mutableListOf(),
) : MutableList<SoopyWaypoint> by waypoints {

    companion object {
        val gson = ConfigManager.gson

        fun fromJson(json: String): SoopyWaypointList = gson.fromJson<SoopyWaypointList>(json)
        fun fromJson(json: JsonElement): SoopyWaypointList = gson.fromJson<SoopyWaypointList>(json)
    }
    fun toJson(): String = gson.toJson(this)
}

class SoopyWaypoint(
    val location: LorenzVec,
    val color: Color = Color(0f, 1f, 0f, 0.4f),
    val options: MutableMap<String, String> = mutableMapOf("name" to ""),
)
