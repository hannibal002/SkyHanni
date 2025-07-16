package at.hannibal2.skyhanni.features.bingo.bingonet

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.BNConnection
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.events.hypixel.HypixelJoinEvent
import de.hype.bingonet.shared.constants.StatusConstants
import de.hype.bingonet.shared.objects.SplashData
import de.hype.bingonet.shared.packets.function.SplashUpdatePacket
import java.util.concurrent.TimeUnit

@Suppress("SkyHanniModuleInspection")
object SplashStatusUpdateListener {
    var splashed: Boolean = false
    var full: Boolean = false
    var data: SplashData? = null
    var maxPlayers: Int = 0
    var isInLobby: Boolean = true

    @HandleEvent
    public fun onHypixelJoin(event: HypixelJoinEvent) {
        val username
        data = SplashManager.splashPool.values.firstOrNull { it?.serverID == HypixelData.serverId && }
        maxPlayers = HypixelData.getMaxPlayersForCurrentServer() - 5
    }

    fun run() {
        val maxPlayerCount: Int = HypixelData.getMaxPlayersForCurrentServer() - 5
        isInLobby.set(true)
        while (isInLobby.get()) {
            if (!full && (HypixelData.getPlayersOnCurrentServer() >= maxPlayerCount)) {
                setStatus(StatusConstants.FULL)
                full = true
            }
            try {
                Thread.sleep(250)
            } catch (ignored: InterruptedException) {
            }
        }
        if (splashed) {
            setStatus(StatusConstants.DONEBAD)
        } else {
            setStatus(StatusConstants.DONEBAD)
        }
    }

    public fun useOverlay(): Boolean {
        return SkyHanniMod.feature.event.bingo.useSplasherOverlay
    }

    fun setStatus(newStatus: StatusConstants) {
        if (data?.status != newStatus) BNConnection.sendPacket(SplashUpdatePacket(data.splashId, newStatus))
        if (newStatus == StatusConstants.SPLASHING) {
            splashed = true
            BingoNet.executionService.schedule(
                {
                    setStatus(StatusConstants.DONEBAD)
                    isInLobby.set(false)
                },
                1, TimeUnit.MINUTES,
            )
        }
        data.status = newStatus
    }


}
