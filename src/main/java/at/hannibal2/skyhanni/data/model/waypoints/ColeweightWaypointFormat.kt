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
            ConfigManager.gson.fromJson<Waypoints<ColeweightWaypoint>>(string, type).transform { it.load() }
        } catch (e: Exception) {
            ChatUtils.debug(e.stackTraceToString())
            null
        }
    }

    private fun ColeweightWaypoint.load() = SkyhanniWaypoint(
        LorenzVec(x, y, z),
        @Suppress("UnsafeCallOnNullableType")
        options["name"]!!.toInt(),
        options,
    )

    override fun canLoad(string: String): Boolean {
        return load(string) != null
    }

    override fun export(waypoints: Waypoints<SkyhanniWaypoint>): String {
        return ConfigManager.gson.toJson(waypoints.transform { it.export() }, Waypoints<ColeweightWaypoint>()::class.java)
    }

    private fun SkyhanniWaypoint.export(): ColeweightWaypoint = with(location) {
        ColeweightWaypoint(
            x.toInt(),
            y.toInt(),
            z.toInt(),
            r = 0.0,
            g = 1.0,
            b = 0.0,
            options,
        )
    }

    override val name: String get() = "coleweight"
}
