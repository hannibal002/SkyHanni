package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.model.SoopyWaypoint
import at.hannibal2.skyhanni.data.model.SoopyWaypointList
import at.hannibal2.skyhanni.utils.json.fromJson
import java.util.Base64
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import kotlin.text.Charsets.UTF_8
import com.google.gson.Gson

data class Waypoint(
    val name: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val enabled: Boolean = true,
    val color: Int = 16711680,
    val addedAt: Long = 1666700977214
)

data class Category(
    val name: String,
    val waypoints: MutableList<Waypoint>,
    val island: String = "crystal_hollows"
)

data class Result(
    val success: Boolean,
    val message: String? = null,
    val waypoints: List<SoopyWaypoint>? = null,
    val categories: List<Category>? = null
)

object WaypointLoader {

    // Main function
    fun getWaypoints(data: String, format: String): Result {
        return try {
            val oldJSON: Map<String, Any>
            val rows: List<String>
            var tempData: Any? = null

            when {
                data.startsWith("<Skytils-Waypoint-Data>(V") -> {
                    return Result(false, "Loader doesn't support Skytils V1 (goto ninjune.dev/waypoint &e)")
                }
                data.startsWith("[{") -> { // soopy from website
                    val tmp: SoopyWaypointList = SoopyWaypointList.fromJson(data)
                }
            }

            when (format) {
                "soopy" -> {
                    Result(true, waypoints = SoopyWaypointList.fromJson(data))
                }
                else -> Result(false)
            }
        } catch (e: Exception) {
            ChatUtils.debug(e.stackTraceToString(), true)
            Result(false, e.message)
        }
    }

    // Helper functions
    fun weirdSoopyFormatToRealSoopyFormat(str: String): Map<String, Map<String, Any>> {
        val byteArray = Base64.getDecoder().decode(str)
        val dataIS = DataInputStream(ByteArrayInputStream(byteArray))

        val dataVersion = dataIS.readByte()
        if (dataVersion.toInt() != 1) {
            throw IllegalArgumentException("Invalid waypoint data version!")
        }

        val json = mutableMapOf<String, Map<String, Any>>()
        val numbWaypoints = dataIS.readInt()

        for (i in 0 until numbWaypoints) {
            val waypointD = mutableMapOf<String, Any>()
            val waypointID = dataIS.readUTF()

            waypointD["x"] = dataIS.readFloat()
            waypointD["y"] = dataIS.readFloat()
            waypointD["z"] = dataIS.readFloat()
            waypointD["r"] = dataIS.readByte() / (255 / 2)
            waypointD["g"] = dataIS.readByte() / (255 / 2)
            waypointD["b"] = dataIS.readByte() / (255 / 2)
            waypointD["area"] = dataIS.readUTF()
            waypointD["options"] = mapOf("name" to dataIS.readUTF())

            json[waypointID] = waypointD
        }

        return json
    }

    fun decodeBase64ToJson(base64: String): Map<String, Any> {
        val decodedBytes = Base64.getDecoder().decode(base64)
        val decodedString = String(decodedBytes, Charsets.UTF_8)
        return Gson().fromJson(decodedString)
    }

    /*fun parseJson(jsonString: String): Map<String, Any> {
        return jsonString.toMap() // Assuming you have a JSON parsing library like Gson or Jackson
    }*/
}

