package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket


/**
 * Used when a Message is broadcast. Requires Mod Privilege.
 */
class BroadcastMessagePacket(val prefix: String?, val username: String?, val message: String?) : AbstractPacket()
