package de.hype.bingonet.shared.packets.function

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.constants.PartyConstants

/**
 * sends the client the request to execute the party command.
 *
 * @see PartyPacket
 */
class PartyPacket
/**
 * @param type         Party Command Type [PartyConstants]
 * @param users        users is just a reference for what behind the type.
 * @param serverBypass true when server did verification for example when hosting bingo party.
 */(val type: PartyConstants, val users: List<String>, val serverBypass: Boolean) : AbstractPacket()
