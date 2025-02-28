package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.WarpsJson
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland

@SkyHanniModule
object ShortenWarpCommand {

    private var warps = listOf<String>()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<WarpsJson>("Warps")
        warps = data.warpCommands
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!SkyHanniMod.feature.misc.commands.shortenWarp) return

        val message = event.message.lowercase()
        if (!message.startsWith("/")) return

        val command = message.removePrefix("/")
        if (command == "barn" && IslandType.GARDEN.isInIsland()) return

        if (command in warps) {
            event.cancel()
            HypixelCommands.warp(command)
        }
    }
}
