package at.hannibal2.skyhanni.features.bingo.bingonet

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.event.bingo.bingonet.network.BNConnection
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import de.hype.bingonet.shared.constants.Islands
import de.hype.bingonet.shared.objects.SplashData
import de.hype.bingonet.shared.packets.function.RequestDynamicSplashInvitePacket
import de.hype.bingonet.shared.packets.function.SplashUpdatePacket
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

@Suppress("SkyHanniModuleInspection")
//Not needed since the SH Message Event is asked for by the Player. Not needed to be a module.
object SplashManager {
    var splashPool: MutableMap<Int?, DisplaySplash?> = HashMap<Int?, DisplaySplash?>()

    fun addSplash(splash: SplashData, source: SplashSource) {
        try {
            splashPool.put(splash.splashId, DisplaySplash(splash))
            DelayedRun.runDelayed(
                5.minutes,
                {
                    splashPool.remove(splash.splashId)
                },
            )
            SplashManager.display(splash.splashId, source)
        } catch (ignored: Exception) {
            //cant happen anyway
        }
    }

    fun handleSplash(data: SplashData, source: SplashSource) {
        addSplash(data, source)
        display(data.splashId, source)
    }

    fun updateSplash(packet: SplashUpdatePacket) {
        val splash = splashPool.get(packet.splashId)
        if (splash != null) {
            splash.status = packet.status
            if (splash.alreadyDisplayed) {
                if (SkyHanniMod.feature.event.bingo.showSplashStatusUpdates && splash.hubSelectorData != null) {
                    ChatUtils.chat("§6The Splash in ${splash.hubSelectorData.hubType.getDisplayName()} §a#${splash.hubSelectorData?.hubNumber ?: splash.serverID}§6 is now §d${packet.status.displayName}§r.")
                }
            }
        }
    }

    enum class SplashSource {
        BN,
        BB
    }

    fun display(splashId: Int, source: SplashSource) {
        val splash = splashPool.get(splashId)
        if (splash == null) return
        var tellraw: String
        if (splash.hubSelectorData == null) {
            ChatUtils.clickableChat(
                "§d${splash.announcer} is Splashing in a §4PRIVATE§r Lobby.",
                {
                    joinParty(splash, source)
                },
            )
            //TODO add keybind activation too
        } else {
            var islandType: String
            if (splash.hubSelectorData.hubType == Islands.DUNGEON_HUB) {
                islandType = "§dDUNGEON HUB§f"
            } else {
                islandType = "Hub"
            }

            ChatUtils.clickableChat(
                "§d${splash.announcer}§r is Splashing in $islandType #${splash.hubSelectorData.hubNumber}§r at ${splash.locationInHub.displayString} §7| §6${splash.extraMessage ?: ""}",
                {
                    prepareHubWarp(splash, source)
                },
            )
            //TODO add keybind activation too
        }
    }

    class DisplaySplash(packet: SplashData) : SplashData(packet) {
        var alreadyDisplayed: Boolean = false
        var receivedTime: Instant? = Instant.now()
    }

    private fun prepareHubWarp(splash: SplashData, source: SplashSource) {
        if (splash.hubSelectorData == null) {
            joinParty(splash, source)
        } else {
            val currentIsland = HypixelData.skyBlockIsland
            if (splash.hubSelectorData.hubType == Islands.HUB) {
                if (
                    currentIsland == IslandType.DUNGEON_HUB ||
                    currentIsland == IslandType.THE_FARMING_ISLANDS ||
                    currentIsland == IslandType.SPIDER_DEN ||
                    currentIsland == IslandType.GOLD_MINES
                ) {
                    //Double warp needed
                    HypixelCommands.warp(Islands.HUB.name)
                }
                HypixelCommands.warp(Islands.HUB.name)
            } else {
                HypixelCommands.warp(splash.hubSelectorData.hubType.warpArgument ?: error("Illegal Island Type."))
            }
        }
    }

    private var awaitingPartyInvite: String? = null
    private fun joinParty(splash: SplashData, source: SplashSource) {
        if (source == SplashSource.BN) {
            BNConnection.sendPacket(RequestDynamicSplashInvitePacket(splash.splashId))
            ChatUtils.chat("Party Request sent.")
            awaitingPartyInvite = splash.announcer
        } else if (source == SplashSource.BB) {
            PartyApi.leaveParty()
            PartyApi.joinParty(splash.announcer)
        }
    }

    @HandleEvent
    fun handlePartyInvite(message: SkyHanniChatEvent) {
        val awaitingPartyInvite = awaitingPartyInvite ?: return
        PartyApi.receivedInvitePattern.matchMatcher(message.message) {
            val name = group("name")
            if (name == awaitingPartyInvite) {
                PartyApi.leaveParty()
                PartyApi.acceptParty(name)
            }
        }
    }
}
