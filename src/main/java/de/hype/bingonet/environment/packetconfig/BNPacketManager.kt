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

object BNPacketManager{
    var packets: MutableList<Packet<out AbstractPacket>> = ArrayList<Packet<out AbstractPacket>>()

    // Method to initialize packet actions
    init {
        initializePacketActions()
    }

    fun getPackets(): MutableList<Packet<out AbstractPacket>> {
        return packets
    }

    // Method to handle a received packet
    fun initializePacketActions() {
        packets.add(
            Packet<BingoChatMessagePacket>(
                BingoChatMessagePacket::class.java,
                BNConnection::onBingoChatMessagePacket,
            ),
        )
        packets.add(
            Packet<BroadcastMessagePacket>(
                BroadcastMessagePacket::class.java,
                BNConnection::onBroadcastMessagePacket,
            ),
        )
        packets.add(
            Packet<DisconnectPacket>(
                DisconnectPacket::class.java,
                BNConnection::onDisconnectPacket,
            ),
        )
        packets.add(
            Packet<InternalCommandPacket>(
                InternalCommandPacket::class.java,
                BNConnection::onInternalCommandPacket,
            ),
        )
        packets.add(
            Packet<InvalidCommandFeedbackPacket>(
                InvalidCommandFeedbackPacket::class.java,
                BNConnection::onInvalidCommandFeedbackPacket,
            ),
        )
        packets.add(Packet<MiningEventPacket>(MiningEventPacket::class.java, BNConnection::onMiningEventPacket))
        packets.add(Packet<PartyPacket>(PartyPacket::class.java, BNConnection::onPartyPacket))
        //        packets.add(new Packet<>(RequestConnectPacket.class, BNConnection::dummy));
        packets.add(Packet<SplashNotifyPacket>(SplashNotifyPacket::class.java, BNConnection::onSplashNotifyPacket))
        packets.add(Packet<SystemMessagePacket>(SystemMessagePacket::class.java, BNConnection::onSystemMessagePacket))
        packets.add(Packet<WelcomeClientPacket>(WelcomeClientPacket::class.java, BNConnection::onWelcomePacket))
        packets.add(
            Packet<RequestAuthentication>(
                RequestAuthentication::class.java,
                BNConnection::onRequestAuthentication,
            ),
        )
        packets.add(Packet<SplashUpdatePacket>(SplashUpdatePacket::class.java, SplashManager::updateSplash))
        packets.add(
            Packet<GetWaypointsPacket>(
                GetWaypointsPacket::class.java,
                BNConnection::onGetWaypointsPacket,
            ),
        )
        packets.add(Packet<WaypointPacket>(WaypointPacket::class.java, BNConnection::onWaypointPacket))
        packets.add(Packet<CompletedGoalPacket>(CompletedGoalPacket::class.java, BNConnection::onCompletedGoalPacket))
        packets.add(Packet<WantedSearchPacket>(WantedSearchPacket::class.java, BNConnection::onWantedSearchPacket))
        packets.add(
            Packet<CommandChatPromptPacket>(
                CommandChatPromptPacket::class.java,
                BNConnection::onCommandChatPromptPacket,
            ),
        )
        packets.add(
            Packet<PacketChatPromptPacket>(
                PacketChatPromptPacket::class.java,
                BNConnection::onPacketChatPromptPacket,
            ),
        )
        packets.add(Packet<PunishedPacket>(PunishedPacket::class.java, BNConnection::onPunishedPacket))
        packets.add(Packet<PlaySoundPacket>(PlaySoundPacket::class.java, BNConnection::onPlaySoundPacket))
        packets.add(
            Packet<MinionDataResponse.RequestMinionDataPacket>(
                MinionDataResponse.RequestMinionDataPacket::class.java,
                BNConnection::onRequestMinionDataPacket,
            ),
        )
        packets.add(Packet<MinionDataResponse>(MinionDataResponse::class.java, BNConnection::dummy))
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
}
