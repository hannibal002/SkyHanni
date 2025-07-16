package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket

/**
 * Server to Client. Displays a message on the Client.
 */
class SystemMessagePacket
/**
 * @param message   the Message
 * @param important whether the message is important
 * @param ping      whether the client shall play the ping sound
 */(val message: String, val important: Boolean, val ping: Boolean) : AbstractPacket()
