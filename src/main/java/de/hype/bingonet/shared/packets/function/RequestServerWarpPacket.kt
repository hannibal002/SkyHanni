package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.environment.packetconfig.AbstractPacket

/**
 * This is limited to Servers that are ch chest registered currently.
 *
 * Some individuals may be able to warp to any sever id as long as a BingoNet user is in it.
 * - The Systems get changed for this automatically and are less efficient since I usually do not track user position data outside of Crystal Hollows.
 */
class RequestServerWarpPacket(
    val serverId: String,
) : AbstractPacket()
