package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.constants.AuthenticationConstants

/**
 * From Client to Server. Tell the Server they want to connect.
 */
class RequestConnectPacket
/**
 * @param mcuuid           the mcuuid of the peron that wants to connect.
 * @param key              api key or client secret for mojang auth. (the 2 part of the joined server id)
 * @param clientApiVersion
 * @param modName
 * @param authType
 */(
    @JvmField val mcuuid: java.util.UUID,
    @JvmField val key: String,
    val mcVersion: String,
    val modVersion: String,
    val modName: String,
    @JvmField val authType: AuthenticationConstants
) : AbstractPacket()
