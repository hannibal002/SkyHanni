package at.hannibal2.hanni.features.commands

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.jsonobjects.repo.WarpsJson
import at.hannibal2.hanni.events.MessageSendToServerEvent
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.events.chat.TabCompletionEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.HypixelCommands

@HanniModule
object ShortenWarpCommand {

    private val config get() = HanniMod.feature.misc.commands
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
        if (command == "barn" && IslandType.GARDEN.isCurrent() && HanniMod.feature.garden.gardenCommands.warpCommands) return

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
