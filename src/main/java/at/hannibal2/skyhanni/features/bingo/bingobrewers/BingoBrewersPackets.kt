package at.hannibal2.skyhanni.features.bingo.bingobrewers

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.features.bingo.bingonet.SplashManager
import at.hannibal2.skyhanni.utils.ChatUtils
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryonet.Client
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.constants.StatusConstants
import de.hype.bingonet.shared.objects.SplashData
import de.hype.bingonet.shared.objects.SplashLocation
import de.hype.bingonet.shared.objects.SplashLocations

object BingoBrewersPackets {
    fun registerPackets(client: Client) {
        val kryo: Kryo = client.kryo
        kryo.register(ConnectionIgn::class.java)
        kryo.register(SplashNotification::class.java)
        kryo.register(java.util.ArrayList::class.java)
        kryo.register(PlayerCount::class.java)
        kryo.register(PlayerCountBroadcast::class.java)
        kryo.register(java.util.HashMap::class.java)
        kryo.register(receiveConstantsOnStartup::class.java)
        kryo.register(requestLbin::class.java)
        kryo.register(sendLbin::class.java)
        kryo.register(sendCHItems::class.java)
        kryo.register(receiveCHItems::class.java)
        kryo.register(SubscribeToCHServer::class.java)
        kryo.register(ChestInfo::class.java)
        kryo.register(CHChestItem::class.java)
        kryo.register(java.util.LinkedHashSet::class.java)
        kryo.register(RequestWarpToServer::class.java)
        kryo.register(BackgroundWarpTask::class.java)
        kryo.register(RegisterToWarpServer::class.java)
        kryo.register(DoneWithWarpTask::class.java)
        kryo.register(CancelWarpRequest::class.java)
        kryo.register(AbortWarpTask::class.java)
        kryo.register(QueuePosition::class.java)
        kryo.register(ServersSummary::class.java)
        kryo.register(UpdateServers::class.java)
        kryo.register(RequestLiveUpdatesForServerInfo::class.java)
        kryo.register(ServerSummary::class.java)
        kryo.register(WarningBannerInfo::class.java)
        kryo.register(ReceiveConstantsOnStartupModern::class.java)
        kryo.register(JoinAlert::class.java)
        kryo.register(WarperInfo::class.java)
        kryo.register(PollQueuePosition::class.java)
    }

    class ConnectionIgn : BingoBrewersPacket<ConnectionIgn>() {
        var hello: String? = null

        override fun execute(packet: ConnectionIgn, client: Client) {
            handleAsUnexpectedPacket()
        }
    }

    abstract class BingoBrewersPacket<T : BingoBrewersPacket<T>> {
        abstract fun execute(packet: T, client: Client)

        fun handleAsUnexpectedPacket() {
            Chat.sendPrivateMessageToSelfDebug("Bingo Net: Received unexpected Bingo Brewers packet. Please Report this to BINGO NET! Packet Type: " + this.javaClass.getSimpleName())
            println(com.google.gson.Gson().toJson(this))
        }

        fun executeUnparsed(packet: BingoBrewersPacket<*>, client: Client) {
            execute(packet as T, client)
        }
    }

    val config = SkyHanniMod.feature.event.bingo.bingoBrewers

    class SplashNotification : BingoBrewersPacket<SplashNotification>() {
        val message: String? = null
        val splasher: String? = null
        val partyHost: String? = null
        val note: MutableList<String>? = null
        val location: String? = null
        val splash: String? = null
        val dungeonHub: Boolean = false

        override fun execute(packet: SplashNotification, client: Client) {
            if (!config.showSplashes) return
            val message: String = packet.message ?: return
            if ((message == "0") && partyHost != null && (partyHost != "No Party")) {
                if (!config.showPrivateSplashes) return
                //TODO Insert chat prompt key
                ChatUtils.clickableChat(
                    "§d$splasher§r is splashing in a §6Private Server§r. Press TODO to join their Party.",
                    {
                        PartyApi.leaveParty()
                        PartyApi.joinParty(partyHost)
                    },
                )
                return
            }
            val regex = "(i)(mega|mini)\\d+[A-Z]+"
            val pattern = java.util.regex.Pattern.compile(regex)
            val matcher = pattern.matcher(packet.message)

            var serverId: String? = null
            var hubNumber: Int? = null
            if (matcher.find()) {
                serverId = matcher.group()
            } else if (packet.message.trim { it <= ' ' }.matches("^\\d+$".toRegex())) {
                hubNumber = packet.message.trim { it <= ' ' }.toInt()
            } else {
                return
            }
            var splashLocation: SplashLocation = SplashLocations.BEA
            val extraMessage = if (note != null) java.lang.String.join("\n", note) else ""
            var hubSelectorData: SplashData.HubSelectorData? = null
            val island: Islands = if (dungeonHub) Islands.DUNGEON_HUB else Islands.HUB
            if (dungeonHub && extraMessage.lowercase(java.util.Locale.getDefault()).contains("sand")) {
                splashLocation = SplashLocation(location, -60, 121, -20)
            }
            if (hubNumber != null) {
                hubSelectorData = SplashData.HubSelectorData(hubNumber, island)
            } else if (serverId != null) {
                val parts: Array<String> =
                    serverId.split("(<=\\D)(=\\d)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (parts.size == 2) {
                    hubNumber = parts[1]!!.toInt()
                    hubSelectorData = SplashData.HubSelectorData(hubNumber, island)
                }
            }
            val splashData: SplashData = SplashData(
                splasher ?: "Some Bingo Brewer Splasher",
                splashLocation,
                extraMessage,
                false,
                serverId,
                hubSelectorData,
                StatusConstants.OPEN,
            )
            splashData.splashId = -(splash!!.toLong() and 0xFFFFFFFFL).toInt()
            SplashManager.handleSplash(splashData, SplashManager.SplashSource.BB)
        }
    }

    class PlayerCount {
        var playerCount: Int = 0
        var IGN: String? = null
        var server: String? = null
    }

    class PlayerCountBroadcast {
        var playerCounts: java.util.HashMap<String, String>? = null
    }

    class receiveConstantsOnStartup {
        var bingoRankCosts: java.util.HashMap<Int, Int>? = null
        var POINTS_PER_BINGO: Int = 0
        var POINTS_PER_BINGO_COMMUNITIES: Int = 0
        var newCHChestItems: java.util.ArrayList<String> = java.util.ArrayList<String>()
        var chItemRegex: String? = null
        var joinAlertTitle: String? = null
        var joinAlertChat: String? = null
        var CHItemOrder: java.util.LinkedHashSet<String> = java.util.LinkedHashSet<String>()
    }


    // Request the lbin of any item on ah/bz by item id
    // If they don't exist, they won't be included in the response
    class requestLbin {
        var items: java.util.ArrayList<String>? = null
    }

    class sendLbin {
        var lbinMap: java.util.HashMap<String, Int>? = null
    }

    class sendCHItems {
        var items: MutableList<CHChestItem> = java.util.ArrayList<CHChestItem>()
        var x: Int = 0
        var y: Int = 0
        var z: Int = 0
        var server: String? = null
        var day: Int = 0
    }

    class CHChestItem {
        var name: String? = null
        var count: String? = null
        var numberColor: Int? = null
        var itemColor: Int? = null
    }

    class SubscribeToCHServer {
        var server: String? = null
        var day: Int = 0
        var unsubscribe: Boolean = false
    }

    class receiveCHItems : BingoBrewersPacket<receiveCHItems>() {
        var chestMap: java.util.ArrayList<ChestInfo>? = null
        var server: String? = null // used to confirm that the server is correct
        var day: Int = 0 // server's last known day
        var lastReceivedDayInfo: Long = Long.MAX_VALUE

        override fun execute(packet: receiveCHItems, client: Client) {
            UpdateListenerManager.onChLobbyDataReceived(packet)
        }
    }

    class ChestInfo {
        var x: Int = 0
        var y: Int = 0
        var z: Int = 0
        var items: java.util.ArrayList<CHChestItem> = java.util.ArrayList<CHChestItem>()
    }

    class RequestWarpToServer {
        var server: String? = null
        var serverType: String? = null // Crystal Hollows, Dwarven Mines, etc.
    }

    class BackgroundWarpTask {
        var server: String? = null // confirm
        var accountsToWarp: java.util.HashMap<String, String>? = null
    }

    class RegisterToWarpServer {
        var server: String? = null
        var unregister: Boolean = false
    }

    class DoneWithWarpTask {
        var successful: Boolean = true
        var ignsWarped: java.util.ArrayList<String> = java.util.ArrayList<String>()
    }

    class CancelWarpRequest {
        var server: String? = null
    }

    class AbortWarpTask {
        var ign: String? = null
        var ineligible: Boolean = false
    }

    class QueuePosition {
        var positionInWarpQueue: Int = 0
    }

    class ServersSummary {
        var serverInfo: java.util.HashMap<String, ServerSummary> =
            java.util.HashMap<String, ServerSummary>()
    }

    class UpdateServers {
        var serversAndLastUpdatedTime: java.util.HashMap<String, Long> =
            java.util.HashMap<String, Long>()
    }

    class RequestLiveUpdatesForServerInfo {
        var unrequest: Boolean = false
    }

    class WarningBannerInfo {
        var text: String? = null
        var textColor: Int = 0xFFFFFF
        var backgroundColor: Int = 0x000000
    }

    class ReceiveConstantsOnStartupModern {
        var constants: java.util.HashMap<String, Any> = java.util.HashMap<String, Any>()
    }

    class JoinAlert {
        var joinAlertChat: String? = null
        var joinAlertTitle: String? = null
    }

    class WarperInfo {
        var ign: String? = null
    }

    class PollQueuePosition {
        var server: String? = null
    }

    class ServerSummary {
        var server: String? = null
        var serverType: String? =
            null // Ex. Crystal Hollows, just so the warp network can be implemented for other servers in the future easily, add a check to make sure it's "Crystal Hollows"
        var availablePlayersToWarp: Int = 0 // how many players are in the lobby that can warp
        var lastUpdated: Long = 0
        var condensedItems: java.util.HashMap<String, Any> =
            java.util.HashMap<String, Any>()
    }
}
