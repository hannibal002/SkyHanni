package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.ClientWaypointData
import de.hype.bingonet.shared.objects.WaypointData

/**
 * Used to tell the addon what message came in.
 */
class GetWaypointsPacket(val waypoints: List<WaypointData>) : AbstractPacket()
