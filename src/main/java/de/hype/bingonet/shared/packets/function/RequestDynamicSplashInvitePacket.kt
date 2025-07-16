package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.environment.packetconfig.AbstractPacket

/**
 * Tells the Server you want to get a invite to the specified splash id.
 */
class RequestDynamicSplashInvitePacket
/**
 * @param splashId The id of the splash you want to get invited to.
 */(@JvmField val splashId: Int) : AbstractPacket()
