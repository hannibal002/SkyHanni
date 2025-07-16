package de.hype.bingonet.shared.packets.service

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.objects.BBServiceData

class ServiceCreatedPacket(val data: BBServiceData) : AbstractPacket()
