package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.chat.TabCompletionEvent
import at.hannibal2.skyhanni.features.commands.PartyCommands
import at.hannibal2.skyhanni.features.commands.ViewRecipeCommand
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object TabComplete {

    @HandleEvent
    fun handleTabComplete(event: TabCompletionEvent) {
        val splits = event.leftOfCursor.split(" ")
        if (splits.size <= 1) return
        var command = splits.first().lowercase()
        if (!command.startsWith("/")) return
        command = command.substring(1)
        customTabComplete(event.leftOfCursor.substring(1), command)?.let {
            event.addSuggestions(it)
        }
    }

    @Suppress("ReturnCount")
    private fun customTabComplete(fullCommand: String, command: String): List<String>? {
        PlayerTabComplete.handleTabComplete(fullCommand)?.let { return it }
        PartyCommands.customTabComplete(command)?.let { return it }
        ViewRecipeCommand.customTabComplete(command)?.let { return it }

        return null
    }
}
