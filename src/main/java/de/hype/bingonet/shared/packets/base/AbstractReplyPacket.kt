package de.hype.bingonet.shared.packets.base

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import java.util.*

open class ExpectReplyPacket<RespondPacket : ExpectReplyPacket.ReplyPacket> protected constructor(
    version: Int,
    minVersion: Int
) : AbstractPacket(version, minVersion) {
    var packetDate: Long = Date().time

    fun preparePacketToReplyToThis(packet: RespondPacket): RespondPacket {
        packet.replyDate = packetDate
        return packet
    }

    open class ReplyPacket : AbstractPacket() {
        var replyDate: Long = -1
    }
}
