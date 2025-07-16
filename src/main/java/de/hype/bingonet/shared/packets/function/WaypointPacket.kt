package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.WaypointData

/**
 * Used to tell the addon what message came in.
 */
class WaypointPacket(val waypoint: WaypointData, val waypointId: Int, val operation: Operation) :
    AbstractPacket() {
    enum class Operation {
        ADD,
        REMOVE,
        EDIT
    }
}
