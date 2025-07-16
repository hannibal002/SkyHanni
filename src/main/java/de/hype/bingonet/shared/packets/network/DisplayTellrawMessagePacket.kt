package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket

/**
 * Used to tell the client to generate a clientside tellraw message.
 * Disabled as of now due to the potential security issue clickable commands are.
 */
class DisplayTellrawMessagePacket(val rawJson: String?) : AbstractPacket()
