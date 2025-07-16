package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.shared.packets.base.ExpectReplyPacket

/**
 * Used by the Server to find the best person to send out an invite.
 */
class RequestPartyStatePacket : ExpectReplyPacket<RequestPartyStatePacket.PartyStatePacket>(1, 1) {
    data class PartyStatePacket(
        val allowServerPartyInvite: Boolean,
        val isInParty: Boolean,
        val allPlayersInLobby: Boolean,
        val currentPartySize: Int,
        val isLeader: Boolean,
        val canInvitePlayers: Boolean
    ) : ReplyPacket() {
        public fun canBeUsedForWarp(): Boolean {
            if (!allowServerPartyInvite) return false
            if (!isInParty) return true
            if (isLeader && allPlayersInLobby) return true
            return false
        }
    }
}
