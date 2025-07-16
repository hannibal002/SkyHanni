package de.hype.bingonet.shared.packets.network

import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.packets.base.ExpectReplyPacket


/**
 * Used to find collect Data across Servers. Can be used to find Users or Lobbies by ID
 */
class WantedSearchPacket : ExpectReplyPacket<WantedSearchPacket.WantedSearchPacketReply> {
    var targetFound: Boolean = true
    val username: String?
    val island: Islands?
    val mega: Boolean?
    val minimumPlayerCount: Int?
    val maximumPlayerCount: Int?
    val serverId: String?


    constructor(
        mcUsername: String?,
        serverId: String?,
        island: Islands? = null,
        mega: Boolean?= null,
        minimumPlayerCount: Int?= null,
        maximumPlayerCount: Int?= null
    ) : super(1, 1) {
        this.username = mcUsername
        this.serverId = serverId
        this.mega = mega
        this.island = island
        this.minimumPlayerCount = minimumPlayerCount
        this.maximumPlayerCount = maximumPlayerCount
    }

    class WantedSearchPacketReply(
        @JvmField var finder: String,
        @JvmField var usernames: List<String>,
        var megaServer: Boolean,
        @JvmField var serverId: String
    ) : ExpectReplyPacket.ReplyPacket() {
        var currentPlayerCount: Int = usernames.size

    }

    companion object {
        fun findMcUser(username: String?): WantedSearchPacket {
            val packet = WantedSearchPacket(username, null)
            packet.targetFound = false
            return packet
        }

        @JvmStatic
        fun findServer(serverId: String?): WantedSearchPacket {
            val packet = WantedSearchPacket(null, serverId)
            packet.targetFound = false
            return packet
        }

        fun findPrivateMega(island: Islands?): WantedSearchPacket {
            val packet = WantedSearchPacket(null, null, island, true, null, 15)
            packet.targetFound = false
            return packet
        }

        fun findPrivateMegaHubServer(): WantedSearchPacket {
            return findPrivateMega(Islands.HUB)
        }
    }
}
