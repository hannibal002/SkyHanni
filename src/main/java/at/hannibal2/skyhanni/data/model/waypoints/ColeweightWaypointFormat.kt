package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.auto.service.AutoService
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import java.awt.Color

@AutoService(WaypointFormat::class)
class ColeweightWaypointFormat : WaypointFormat {
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

    override fun load(string: String): Waypoints<SkyhanniWaypoint>? {
        val type = object : TypeToken<Waypoints<ColeweightWaypoint>>() {}.type

        return try {
            Waypoints(
                ConfigManager.gson.fromJson<Waypoints<ColeweightWaypoint>>(string, type)
                    .map {
                        SkyhanniWaypoint(
                            LorenzVec(it.x, it.y, it.z),
                            Color(it.r.toFloat(), it.g.toFloat(), it.b.toFloat(), 0.4f),
                            it.options["name"]!!.toInt(),
                            it.options,
                        )
                    }
                    .toMutableList()
            )
        } catch (e: Exception) {
            ChatUtils.debug(e.stackTraceToString())
            null
        }
    }

    override fun canLoad(string: String): Boolean {
        return load(string) != null
    }

    override fun save(waypoints: Waypoints<SkyhanniWaypoint>): String {
        return ConfigManager.gson.toJson(
            Waypoints(
                waypoints.map {
                    ColeweightWaypoint(
                        it.location.x.toInt(),
                        it.location.y.toInt(),
                        it.location.z.toInt(),
                        it.color.red.toDouble() / 255,
                        it.color.green.toDouble() / 255,
                        it.color.blue.toDouble() / 255,
                        it.options,
                    )
                }.toMutableList(),
            ),
            Waypoints<ColeweightWaypoint>()::class.java,
        )
    }

    override val name: String
        get() = "Coleweight"
}
