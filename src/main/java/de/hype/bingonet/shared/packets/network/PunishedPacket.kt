package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import java.time.Instant

class PunishedPacket(
    val canUseNetwork: Boolean,
    val punishmentType: String,
    val expirationDate: Instant,
) : AbstractPacket()
