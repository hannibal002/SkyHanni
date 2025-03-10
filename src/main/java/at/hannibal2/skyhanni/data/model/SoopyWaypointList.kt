package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters.registerTypeAdapter

@JvmInline
value class SoopyWaypointList(
    @Expose val waypoints: List<SoopyWaypoint>,
) : List<SoopyWaypoint> by waypoints {

    companion object {
        val gson = GsonBuilder().setPrettyPrinting().registerTypeAdapter<SoopyWaypointList>(
            { out, value ->
                out.beginArray()
                value.forEach { waypoint ->
                    out.beginObject()
                    out.name("x").value(waypoint.x)
                    out.name("y").value(waypoint.y)
                    out.name("z").value(waypoint.z)
                    out.name("r").value(waypoint.r)
                    out.name("g").value(waypoint.g)
                    out.name("b").value(waypoint.b)

                    out.name("options").beginObject()
                    waypoint.options.forEach { (key, value) ->
                        out.name(key).value(value)
                    }
                    out.endObject()

                    out.endObject()
                }
                out.endArray()
            },
            { reader ->
                reader.beginArray()
                val waypoints = mutableListOf<SoopyWaypoint>()
                while (reader.hasNext()) {
                    reader.beginObject()
                    var x = 0
                    var y = 0
                    var z = 0
                    var r = 0
                    var g = 1
                    var b = 0
                    val options = mutableMapOf<String, String>()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "x" -> x = reader.nextDouble().toInt()
                            "y" -> y = reader.nextDouble().toInt()
                            "z" -> z = reader.nextDouble().toInt()
                            "r" -> r = reader.nextInt()
                            "g" -> g = reader.nextInt()
                            "b" -> b = reader.nextInt()
                            "options" -> {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    options[reader.nextName()] = reader.nextString()
                                }
                                reader.endObject()
                            }
                            else -> reader.skipValue()
                        }
                    }
                    waypoints.add(SoopyWaypoint(x, y, z, r, g, b, options))
                    reader.endObject()
                }
                reader.endArray()
                SoopyWaypointList(waypoints.toList())
            },
        ).create()

        fun fromJson(json: String): SoopyWaypointList = gson.fromJson(json)
        fun fromJson(json: JsonElement): SoopyWaypointList = gson.fromJson(json)
    }

    fun toJson(): String = gson.toJson(this)
}

data class SoopyWaypoint(
    val x: Int,
    val y: Int,
    val z: Int,
    val r: Int = 0,
    val g: Int = 1,
    val b: Int = 0,
    val options: MutableMap<String, String> = mutableMapOf("name" to ""),
)

fun SoopyWaypoint.toLorenzVec() = LorenzVec(x, y, z)
