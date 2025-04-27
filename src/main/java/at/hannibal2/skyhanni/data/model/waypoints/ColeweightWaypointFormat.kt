package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.auto.service.AutoService
import com.google.gson.annotations.Expose
import java.awt.Color

@AutoService(WaypointFormat::class)
class ColeweightWaypointFormat : WaypointFormat {
    @Suppress("LongParameterList")
    class ColeweightWaypoint(
        @Expose
        val x: Int,
        @Expose
        val y: Int,
        @Expose
        val z: Int,
        @Expose
        val r: Double,
        @Expose
        val g: Double,
        @Expose
        val b: Double,
        @Expose
        val options: MutableMap<String, String> = mutableMapOf(),
    )

    override fun load(string: String): Collection<SkyhanniWaypoint>? {
        return try {
            ConfigManager.gson.fromJson(string, Waypoints<ColeweightWaypoint>()::class.java).map {
                SkyhanniWaypoint(
                    LorenzVec(it.x, it.y, it.z),
                    Color(it.r.toFloat(), it.g.toFloat(), it.b.toFloat()),
                    it.options["name"]!!.toInt(),
                    it.options
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun canLoad(string: String): Boolean {
        return load(string) != null
    }

    override fun save(waypoints: Collection<SkyhanniWaypoint>): String {
        return ConfigManager.gson.toJson(
            Waypoints(
                waypoints.map {
                    ColeweightWaypoint(
                        it.location.x.toInt(),
                        it.location.y.toInt(),
                        it.location.z.toInt(),
                        it.color.red.toDouble(),
                        it.color.green.toDouble(),
                        it.color.blue.toDouble(),
                        it.options
                    )
                }.toMutableList()
            ),
            Waypoints<SkyhanniWaypoint>()::class.java
        )
    }

    override val name: String
        get() = "Coleweight"
}
