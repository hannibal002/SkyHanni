package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.ClientWaypointData

/**
 * Used to tell the addon what message came in.
 */
class GetWaypointsPacket(val waypoints: MutableList<ClientWaypointData>) : AbstractPacket()
