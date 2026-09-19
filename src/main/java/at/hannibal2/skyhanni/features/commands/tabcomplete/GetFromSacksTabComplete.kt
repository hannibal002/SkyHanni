package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.chat.TabCompletionEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SkyBlockUtils

@SkyHanniModule
object GetFromSacksTabComplete {

    private val config get() = SkyHanniMod.feature.misc.commands.tabComplete

    @HandleEvent
    fun onTabComplete(event: TabCompletionEvent) {
        if (!isEnabled()) return
        if (GetFromSackApi.commands.none { event.isCommand(it) }) return
        if (!event.leftOfCursor.contains(" ")) return

        val query = event.lastWord
        val matching = SackApi.sackListNames
            .filter { it.contains(query, ignoreCase = true) }
            .map { it.replace(" ", "_") }
        event.addSuggestionsUnchecked(matching)
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onMessageToServer(event: MessageSendToServerEvent) {
        if (SkyBlockUtils.isOnAlphaServer) return
        if (!event.isCommand(GetFromSackApi.commandsWithSlash)) return
        val replacedEvent = handleUnderlineReplace(event)
        GetFromSackApi.queuedHandler(replacedEvent)
        GetFromSackApi.bazaarHandler(replacedEvent)
        if (replacedEvent.isCancelled) {
            event.cancel()
            return
        }
        if (replacedEvent !== event) {
            event.cancel()
            ChatUtils.sendMessageToServer(replacedEvent.message)
        }
    }

    private fun handleUnderlineReplace(event: MessageSendToServerEvent): MessageSendToServerEvent {
        if (!isEnabled()) return event

        if (event.senderIsSkyhanni()) return event

        if (event.splitMessage.size < 3) return event

        val rawName = event.splitMessage.drop(1).dropLast(1).joinToString(" ")
        val realName = rawName.replace("_", " ")
        if (realName == rawName) return event
        if (realName.uppercase() !in SackApi.sackListNames) return event
        return event.eventWithNewMessage(event.message.replace(rawName, realName))
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.gfsSack
}
