package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.data.model.SoopyWaypointList

object WaypointLoader {
    fun getWaypoints(data: String, format: String = "soopy"): SoopyWaypointList? {
        return try {
            when (format) {
                "soopy" -> {
                    SoopyWaypointList.fromJson(data)
                }
                // TODO: Add other formats (like skytils, old soopy, etc.) maybe
                else -> null
            }
        } catch (e: Exception) {
            ChatUtils.chat("An error occurred while parsing the waypoints ${e.message}")
            null
        }
    }
}

