package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.constants.InternalReasonConstants

/**
 * Used to tell a client that they were disconnected by the Host.
 */
class DisconnectPacket
/**
 * @param internalReason      Reason for the disconnect [InternalReasonConstants]
 * @param waitBeforeReconnect Time before client shall try to reconnect.
 * @param randomExtraDelay    Max random delay that is added to put less stress on the Server
 * @param displayReason       Reason to be displayed why Client was disconnected
 * @param displayMessage      Message is shown on the client.
 */(
    val internalReason: InternalReasonConstants?,
    val waitBeforeReconnect: IntArray,
    val randomExtraDelay: Int,
    val displayReason: String?,
    val displayMessage: String?
) : AbstractPacket()
