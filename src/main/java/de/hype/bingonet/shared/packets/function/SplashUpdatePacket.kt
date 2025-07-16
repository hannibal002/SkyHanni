package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.constants.StatusConstants

/**
 * Client and Server. Updates Splash Status.
 */
class SplashUpdatePacket
/**
 * @param splashId id of the splash
 * @param status   one of the following types: [StatusConstants.WAITING], [StatusConstants.FULL], [StatusConstants.SPLASHING], [StatusConstants.CANCELED], [StatusConstants.DONEBAD]
 */(@JvmField val splashId: Int, @JvmField val status: StatusConstants) : AbstractPacket()
