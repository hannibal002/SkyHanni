package de.hype.bingonet.shared.packets.service

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.constants.TradeType

class RequestServiceTradePacket(@JvmField val tradeType: TradeType) : AbstractPacket()
