package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket


/**
 * From Server to client telling him to authenticate.
 */
class RequestAuthentication
/**
 * @param serverIdSuffix needed for Mojang Auth. "client" + "server" = serverid at mojang.
 * @param serverVersion  the version the server is on.
 */(val serverIdSuffix: String?, val serverVersion: Int) : AbstractPacket()
