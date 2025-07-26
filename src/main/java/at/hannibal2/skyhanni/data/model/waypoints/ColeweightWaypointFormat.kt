package at.hannibal2.skyhanni.data.model.waypoints

import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.KSerializable
import at.hannibal2.skyhanni.utils.LorenzVec
import com.google.auto.service.AutoService
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import io.github.notenoughupdates.moulconfig.ChromaColour

@AutoService(WaypointFormat::class)
@KSerializable
class ColeweightWaypointFormat : WaypointFormat {
    data class ColeweightWaypoint(
        @Expose override val x: Int,
        @Expose override val y: Int,
        @Expose override val z: Int,
        // Because these come as double from Coleweight, we privatize these
        @Expose @SerializedName("r") private val rDouble: Double? = null,
        @Expose @SerializedName("g") private val gDouble: Double? = null,
        @Expose @SerializedName("b") private val bDouble: Double? = null,
        @Expose val options: MutableMap<String, String> = mutableMapOf(),
    ) : AbstractXYZWaypoint(x, y, z), AbstractColoredWaypoint {
        private val compList = listOf(rDouble, gDouble, bDouble)
        private val allUnder = compList.none { it != null && it > 1 }
        private val needMultiplier = (null !in listOf(rDouble, gDouble, bDouble)) && allUnder
        override val color by lazy {
            val (r, g, b) = compList.mapNotNull {
                if (needMultiplier && it != null) (it.toFloat() * 255.0f) else it?.toFloat()
            }.takeIf { it.size == 3 }?.map { it.toInt() } ?: listOf(0, 255, 0)
            ChromaColour.fromStaticRGB(r, g, b, a = 255)
        }

        override fun duplicate(): AbstractXYZWaypoint = copy()
    }

    override fun deserialize(string: String): WaypointSet<SkyhanniWaypoint>? {
        val type = object : TypeToken<WaypointSet<ColeweightWaypoint>>() {}.type
        return try {
            ConfigManager.gson.fromJson<WaypointSet<ColeweightWaypoint>>(string, type).transform { it.load() }
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

    override fun canSerialize(string: String): Boolean {
        return deserialize(string) != null
    }

    override fun serialize(waypoints: WaypointSet<SkyhanniWaypoint>): String {
        return ConfigManager.gson.toJson(waypoints.transform { it.export() }, WaypointSet<ColeweightWaypoint>()::class.java)
    }

    private fun SkyhanniWaypoint.export(): ColeweightWaypoint = with(location) {
        ColeweightWaypoint(
            x.toInt(),
            y.toInt(),
            z.toInt(),
            rDouble = 0.0,
            gDouble = 1.0,
            bDouble = 0.0,
            options,
        )
    }

    override val name: String get() = "coleweight"
}
