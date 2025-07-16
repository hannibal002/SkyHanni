package de.hype.bingonet.shared.packets.service

import de.hype.bingonet.environment.packetconfig.AbstractPacket

class ServiceKickMessagePacket(val serviceId: Int, val suggestRejoin: Boolean, val message: String?) :
    AbstractPacket()
