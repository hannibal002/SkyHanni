package at.hannibal2.skyhanni.features.misc.tabcomplete

import at.hannibal2.skyhanni.features.commands.PartyCommands
import at.hannibal2.skyhanni.features.misc.CollectionTracker

object TabComplete {

    @JvmStatic
    fun handleTabComplete(leftOfCursor: String, originalArray: Array<String>): Array<String>? {
        val splits = leftOfCursor.split(" ")
        if (splits.size > 1) {
            var command = splits.first().lowercase()
            if (command.startsWith("/")) {
                command = command.substring(1)
                customTabComplete(command)?.let {
                    return buildResponse(splits, it).toSet().toTypedArray()
                }
            }
        }
        return null
    }

    private fun customTabComplete(command: String): List<String>? {
        GetFromSacksTabComplete.handleTabComplete(command)?.let { return it }
        WarpTabComplete.handleTabComplete(command)?.let { return it }
        PlayerTabComplete.handleTabComplete(command)?.let { return it }
        CollectionTracker.handleTabComplete(command)?.let { return it }
        PartyCommands.customTabComplete(command)?.let { return it }

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
