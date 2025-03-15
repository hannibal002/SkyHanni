package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.annotations.Expose
import at.hannibal2.skyhanni.utils.json.SkyHanniTypeAdapters.registerTypeAdapter
import com.google.gson.Gson


class SoopyWaypointList(
    @Expose val waypoints: List<SoopyWaypoint> = listOf(),
) : List<SoopyWaypoint> by waypoints {
    companion object {
        val gson: Gson = GsonBuilder().setPrettyPrinting().registerTypeAdapter<SoopyWaypointList>(
            { out, value ->
                out.beginArray()
                value.forEach { waypoint ->
                    out.beginObject()
                    out.name("x").value(waypoint.x)
                    out.name("y").value(waypoint.y)
                    out.name("z").value(waypoint.z)
                    out.name("r").value(waypoint.r / 255.0)
                    out.name("g").value(waypoint.g / 255.0)
                    out.name("b").value(waypoint.b / 255.0)

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
                    var g = 255
                    var b = 0
                    val options = mutableMapOf<String, String>()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "x" -> x = reader.nextInt()
                            "y" -> y = reader.nextInt()
                            "z" -> z = reader.nextInt()
                            "r" -> r = (reader.nextDouble() * 255).toInt()
                            "g" -> g = (reader.nextDouble() * 255).toInt()
                            "b" -> b = (reader.nextDouble() * 255).toInt()
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
                    val waypoint = SoopyWaypoint(x, y, z, r, g, b, options)
                    waypoints.add(waypoint)
                    reader.endObject()
                }
                reader.endArray()
                SoopyWaypointList(waypoints)
            },
        ).create()

        fun fromJson(json: String): SoopyWaypointList = gson.fromJson<SoopyWaypointList>(json)
        fun fromJson(json: JsonElement): SoopyWaypointList = gson.fromJson<SoopyWaypointList>(json)
    }

    fun toJson(): String = gson.toJson(this)
}

class SoopyWaypoint(
    val x: Int,
    val y: Int,
    val z: Int,
    val r: Int = 0,
    val g: Int = 255,
    val b: Int = 0,
    val options: MutableMap<String, String> = mutableMapOf("name" to ""),
)

fun SoopyWaypoint.toLorenzVec() = LorenzVec(x, y, z)
