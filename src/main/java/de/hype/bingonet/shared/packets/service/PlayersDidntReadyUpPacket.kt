package de.hype.bingonet.shared.packets.service

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.server.objects.BBUser

/**
 * send when the timeout for the ready up has been reached and not everyone readied up.
 */
class PlayersDidntReadyUpPacket(var serverId: Int, var participiants: MutableSet<BBUser>) : AbstractPacket()
