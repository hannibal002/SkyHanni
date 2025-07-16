package de.hype.bingonet.shared.packets.mining

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.ChChestData

/**
 * Client and Server. Updates ChChests Status.
 */
class ChestLobbyUpdatePacket
    (@JvmField val serverId: String, val chests: List<ChChestData>) : AbstractPacket()
