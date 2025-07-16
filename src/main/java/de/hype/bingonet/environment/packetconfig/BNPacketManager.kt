package at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.environment.packetconfig

import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.BNConnection
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.features.bingo.bingonet.SplashManager
import de.hype.bingonet.environment.packetconfig.AbstractPacket
import de.hype.bingonet.shared.packets.function.CommandChatPromptPacket
import de.hype.bingonet.shared.packets.function.GetWaypointsPacket
import de.hype.bingonet.shared.packets.function.MinionDataResponse
import de.hype.bingonet.shared.packets.function.PacketChatPromptPacket
import de.hype.bingonet.shared.packets.function.PartyPacket
import de.hype.bingonet.shared.packets.function.PlaySoundPacket
import de.hype.bingonet.shared.packets.function.RequestPartyStatePacket
import de.hype.bingonet.shared.packets.function.SplashNotifyPacket
import de.hype.bingonet.shared.packets.function.SplashUpdatePacket
import de.hype.bingonet.shared.packets.function.WaypointPacket
import de.hype.bingonet.shared.packets.mining.ChChestPacket
import de.hype.bingonet.shared.packets.mining.ChestLobbyUpdatePacket
import de.hype.bingonet.shared.packets.mining.MiningEventPacket
import de.hype.bingonet.shared.packets.network.BingoChatMessagePacket
import de.hype.bingonet.shared.packets.network.BroadcastMessagePacket
import de.hype.bingonet.shared.packets.network.CompletedGoalPacket
import de.hype.bingonet.shared.packets.network.DisconnectPacket
import de.hype.bingonet.shared.packets.network.InternalCommandPacket
import de.hype.bingonet.shared.packets.network.InvalidCommandFeedbackPacket
import de.hype.bingonet.shared.packets.network.PunishedPacket
import de.hype.bingonet.shared.packets.network.RequestAuthentication
import de.hype.bingonet.shared.packets.network.SystemMessagePacket
import de.hype.bingonet.shared.packets.network.WantedSearchPacket
import de.hype.bingonet.shared.packets.network.WelcomeClientPacket

class BNPacketManager(// Define a map to store packet classes and their associated actions
    var connection: BNConnection?
) {
    var packets: MutableList<Packet<out AbstractPacket>> = ArrayList<Packet<out AbstractPacket>>()

    // Method to initialize packet actions
    init {
        initializePacketActions(connection)
        lastBNPacketManager = this
    }

    fun getPackets(): MutableList<Packet<out AbstractPacket>> {
        return packets
    }

    // Method to handle a received packet
    fun initializePacketActions(connection: BNConnection?) {
        packets.add(
            Packet<BingoChatMessagePacket>(
                BingoChatMessagePacket::class.java,
                connection::onBingoChatMessagePacket,
            ),
        )
        packets.add(
            Packet<BroadcastMessagePacket>(
                BroadcastMessagePacket::class.java,
                connection::onBroadcastMessagePacket,
            ),
        )
        packets.add(
            Packet<DisconnectPacket>(
                DisconnectPacket::class.java,
                connection::onDisconnectPacket,
            ),
        )
        packets.add(
            Packet<InternalCommandPacket>(
                InternalCommandPacket::class.java,
                connection::onInternalCommandPacket,
            ),
        )
        packets.add(
            Packet<InvalidCommandFeedbackPacket>(
                InvalidCommandFeedbackPacket::class.java,
                connection::onInvalidCommandFeedbackPacket,
            ),
        )
        packets.add(Packet<MiningEventPacket>(MiningEventPacket::class.java, connection::onMiningEventPacket))
        packets.add(Packet<PartyPacket>(PartyPacket::class.java, connection::onPartyPacket))
        //        packets.add(new Packet<>(RequestConnectPacket.class, connection::dummy));
        packets.add(Packet<SplashNotifyPacket>(SplashNotifyPacket::class.java, connection::onSplashNotifyPacket))
        packets.add(Packet<SystemMessagePacket>(SystemMessagePacket::class.java, connection::onSystemMessagePacket))
        packets.add(Packet<WelcomeClientPacket>(WelcomeClientPacket::class.java, connection::onWelcomePacket))
        packets.add(
            Packet<RequestAuthentication>(
                RequestAuthentication::class.java,
                connection::onRequestAuthentication,
            ),
        )
        packets.add(Packet<SplashUpdatePacket>(SplashUpdatePacket::class.java, SplashManager::updateSplash))
        packets.add(
            Packet<GetWaypointsPacket>(
                GetWaypointsPacket::class.java,
                connection::onGetWaypointsPacket,
            ),
        )
        packets.add(Packet<WaypointPacket>(WaypointPacket::class.java, connection::onWaypointPacket))
        packets.add(Packet<CompletedGoalPacket>(CompletedGoalPacket::class.java, connection::onCompletedGoalPacket))
        packets.add(Packet<WantedSearchPacket>(WantedSearchPacket::class.java, connection::onWantedSearchPacket))
        packets.add(
            Packet<CommandChatPromptPacket>(
                CommandChatPromptPacket::class.java,
                connection::onCommandChatPromptPacket,
            ),
        )
        packets.add(
            Packet<PacketChatPromptPacket>(
                PacketChatPromptPacket::class.java,
                connection::onPacketChatPromptPacket,
            ),
        )
        packets.add(Packet<PunishedPacket>(PunishedPacket::class.java, connection::onPunishedPacket))
        packets.add(Packet<PlaySoundPacket>(PlaySoundPacket::class.java, connection::onPlaySoundPacket))
        packets.add(
            Packet<MinionDataResponse.RequestMinionDataPacket>(
                MinionDataResponse.RequestMinionDataPacket::class.java,
                connection::onRequestMinionDataPacket,
            ),
        )
        packets.add(Packet<MinionDataResponse>(MinionDataResponse::class.java, connection::dummy))
        packets.add(
            Packet<RequestPartyStatePacket>(
                RequestPartyStatePacket::class.java,
                PartyApi::onRequestPartyStatePacket,
            ),
        )
        packets.add(
            Packet<ChChestPacket>(
                ChChestPacket::class.java,
                UpdateListenerManager::onChChestDataReceived,
            ),
        )
        packets.add(
            Packet<ChestLobbyUpdatePacket>(
                ChestLobbyUpdatePacket::class.java,
                (UpdateListenerManager::onChLobbyDataReceived),
            ),
        )
    }

    companion object {
        private var lastBNPacketManager: BNPacketManager? = null
        val allPacketClasses: MutableList<Class<out AbstractPacket>>
            //   method to get a list of all packets
            get() {
                if (lastBNPacketManager == null) {
                    lastBNPacketManager = BNPacketManager(null)
                }
                val allPackets: MutableList<Class<out AbstractPacket>> =
                    ArrayList<Class<out AbstractPacket>>()
                for (i in lastBNPacketManager!!.packets.indices) {
                    allPackets.add(lastBNPacketManager!!.packets.get(i)!!.clazz)
                }
                return allPackets
            }
    }
}
