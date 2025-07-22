package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.auto.service.AutoService
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken

@AutoService(WaypointFormat::class)
class ColeweightWaypointFormat : WaypointFormat {
    data class ColeweightWaypoint(
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
    ) : Copyable<ColeweightWaypoint> {
        override fun copy() = ColeweightWaypoint(x, y, z, r, g, b, options)
    }

    override fun load(string: String): Waypoints<SkyhanniWaypoint>? {
        val type = object : TypeToken<Waypoints<ColeweightWaypoint>>() {}.type

        return try {
            Waypoints(
                ConfigManager.gson.fromJson<Waypoints<ColeweightWaypoint>>(string, type)
                    .map {
                        SkyhanniWaypoint(
                            LorenzVec(it.x, it.y, it.z),
                            @Suppress("UnsafeCallOnNullableType")
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

    override fun export(waypoints: Waypoints<SkyhanniWaypoint>): String {
        return ConfigManager.gson.toJson(
            Waypoints(
                waypoints.map {
                    ColeweightWaypoint(
                        it.location.x.toInt(),
                        it.location.y.toInt(),
                        it.location.z.toInt(),
                        0.0,
                        1.0,
                        0.0,
                        it.options,
                    )
                }.toMutableList(),
            ),
            Waypoints<ColeweightWaypoint>()::class.java,
        )
    }

    override val name: String
        get() = "coleweight"
}
