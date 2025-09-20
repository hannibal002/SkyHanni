package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.chat.TabCompletionEvent
import at.hannibal2.skyhanni.features.misc.AbiphoneFeatures
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.HypixelCommands.call


@SkyHanniModule
object ShortenCallCommand {

    private val config get() = SkyHanniMod.feature.misc.commands

    @HandleEvent(onlyOnSkyblock = true)
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!config.shortenCall) return

        val message = event.message
        if (!message.startsWith("/")) return

        val command = message.lowercase().removePrefix("/").trimEnd()

        val contacts = AbiphoneFeatures.abiphoneContacts
        if ((contacts != null) && (command in contacts)) {
            if (!config.shortenForgeToCallNotWarp && command == "forge") return
            event.cancel()
            HypixelCommands.call(command)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabComplete(event: TabCompletionEvent) {
        if (!config.shortenCall) return

        if (event.leftOfCursor.contains(" ")) return

        val lastWord = event.lastWord.lowercase().removePrefix("/")

        val contacts = AbiphoneFeatures.abiphoneContacts
        if (contacts != null) {
            val matchingCalls = contacts.filter { it.startsWith(lastWord) }.map { "/$it" }
            event.addSuggestions(matchingCalls)
        }
    }
}
