package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.jsonobjects.repo.WarpsJson
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.TabCompletionEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands

@SkyHanniModule
object ShortenWarpCommand {

    private val config get() = SkyHanniMod.feature.misc.commands
    private var warps = emptyList<String>()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<WarpsJson>("Warps")
        warps = data.warpCommands
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!config.shortenWarp) return

        val message = event.message
        if (!message.startsWith("/")) return

        val command = message.lowercase().removePrefix("/").trimEnd()
        // Avoid overriding commands on islands where they have a different use
        if (command == "jerry" && IslandType.PRIVATE_ISLAND.isCurrent()) return
        if (command == "barn" && IslandType.GARDEN.isCurrent() && SkyHanniMod.feature.garden.gardenCommands.warpCommands) return

        if (command in warps) {
            event.cancel()
            HypixelCommands.warp(command)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabComplete(event: TabCompletionEvent) {
        if (!config.shortenWarp) return

        if (event.leftOfCursor.contains(" ")) return

        val lastWord = event.lastWord.lowercase().removePrefix("/")
        val matchingWarps = warps.filter { it.startsWith(lastWord) }.map { "/$it" }

        event.addSuggestions(matchingWarps)
    }
}
