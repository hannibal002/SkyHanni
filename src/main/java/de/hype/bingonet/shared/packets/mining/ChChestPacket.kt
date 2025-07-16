package de.hype.bingonet.shared.packets.mining

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.ChChestData

/**
 * Used to announce a found CHChest. Can be from Client to Server to announce global or from Server to Client for the public announce.
 */
class ChChestPacket
/**
 * @param chest [ChestLobbyData] object containing the data
 */(@JvmField val chest: ChChestData, val server: String) : AbstractPacket()
