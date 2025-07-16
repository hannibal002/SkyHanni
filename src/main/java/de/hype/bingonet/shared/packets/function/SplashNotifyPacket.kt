package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.SplashData

/**
 * Server and Client. USed to announce to each other.
 */
class SplashNotifyPacket
/**
 * @param splash [SplashData] is used for storing all the Information
 */(@JvmField val splash: SplashData) : AbstractPacket()
