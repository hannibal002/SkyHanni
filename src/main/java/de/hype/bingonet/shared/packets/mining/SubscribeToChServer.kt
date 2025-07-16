package de.hype.bingonet.shared.packets.mining

import de.hype.bingonet.environment.packetconfig.AbstractPacket
import java.time.Instant

/**
 * Used to tell the Server that you want to subscribe to info for that server. Also used to know whether your in the server for other warp ins etc.
 */
class SubscribeToChServer
/**
 * @param chest [ChestLobbyData] object containing the data
 */(val server: String, val closingTime: Instant) : AbstractPacket()


/**
 * Used to tell the Server that you want to unsubscribe from info for that server. The Player List will be used to try to make it so that other players can still try to get into the lobby by asking non Bingo Net users manually.
 */
class UnSubscribeToChServer
/**
 * @param chest [ChestLobbyData] object containing the data
 */(val server: String, val players: Set<String>) : AbstractPacket()
