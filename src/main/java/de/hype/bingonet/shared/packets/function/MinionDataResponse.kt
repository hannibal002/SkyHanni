package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.shared.compilation.sbenums.minions.Minion
import de.hype.bingonet.shared.packets.base.ExpectReplyPacket

class MinionDataResponse(val minions: MutableMap<Minion, Int>?, val maxSlots: Int) : ExpectReplyPacket.ReplyPacket() {
    class RequestMinionDataPacket : ExpectReplyPacket<MinionDataResponse>(1, 1)
}
