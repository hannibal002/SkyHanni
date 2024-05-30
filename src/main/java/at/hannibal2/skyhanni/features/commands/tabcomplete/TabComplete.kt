package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.events.TabCompletionEvent
import at.hannibal2.skyhanni.features.commands.PartyCommands
import at.hannibal2.skyhanni.features.commands.ViewRecipeCommand
import at.hannibal2.skyhanni.features.misc.CollectionTracker
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TabComplete {

    @SubscribeEvent
    fun handleTabComplete(event: TabCompletionEvent) {
        val splits = event.leftOfCursor.split(" ")
        if (splits.size > 1) {
            var command = splits.first().lowercase()
            if (command.startsWith("/")) {
                command = command.substring(1)
                customTabComplete(command)?.let {
                    event.addSuggestions(it)
                }
            }
        }
    }

    private fun customTabComplete(command: String): List<String>? {
        GetFromSacksTabComplete.handleTabComplete(command)?.let { return it }
        PlayerTabComplete.handleTabComplete(command)?.let { return it }
        CollectionTracker.handleTabComplete(command)?.let { return it }
        PartyCommands.customTabComplete(command)?.let { return it }
        ViewRecipeCommand.customTabComplete(command)?.let { return it }

        return null
    }

    private fun buildResponse(arguments: List<String>, fullResponse: List<String>): List<String> {
        if (arguments.size == 2) {
            val start = arguments[1].lowercase()
            return fullResponse.filter { it.lowercase().startsWith(start) }
        }
        return emptyList()
    }
}
