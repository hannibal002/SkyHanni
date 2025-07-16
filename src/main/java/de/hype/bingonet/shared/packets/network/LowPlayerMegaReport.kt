package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket

class LowPlayerMegaReport(@JvmField val playerCount: Int, @JvmField val serverId: String?) : AbstractPacket()
