package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket

//Only used for small things which don't really need an own Packet.
/**
 * Several functions for the client to execute on included scripts.
 * See the Constants in code for explanations.
 */
class InternalCommandPacket(@JvmField val command: InternalCommand?, @JvmField val parameters: Array<String>) :
    AbstractPacket() {
//TODO change everything to own packets (deactivate bn integration, get pot time)

}
