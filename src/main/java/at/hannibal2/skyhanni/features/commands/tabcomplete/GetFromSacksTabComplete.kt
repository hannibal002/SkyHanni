package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.GetFromSackApi.commands
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SackApi
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.chat.TabCompletionEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils.eventWithNewMessage
import at.hannibal2.skyhanni.utils.ChatUtils.senderIsSkyhanni
import at.hannibal2.skyhanni.utils.SkyBlockUtils

@SkyHanniModule
object GetFromSacksTabComplete {

    private val config get() = SkyHanniMod.feature.misc.commands.tabComplete

    @HandleEvent
    fun onTabComplete(event: TabCompletionEvent) {
        if (!isEnabled()) return
        if (commands.none { event.isCommand(it) }) return
        if (!event.leftOfCursor.contains(" ")) return

        val query = event.lastWord
        val matching = SackApi.sackListNames
            .filter { it.contains(query, ignoreCase = true) }
            .map { it.replace(" ", "_") }
        event.addSuggestionsUnchecked(matching)
    }

    // No subscribe since it needs to be called from the GetFromSackAPI
    fun handleUnderlineReplace(event: MessageSendToServerEvent): MessageSendToServerEvent {
        if (!isEnabled()) return event

        if (event.senderIsSkyhanni()) return event

        if (event.splitMessage.size < 3) return event

        val rawName = event.splitMessage.drop(1).dropLast(1).joinToString(" ")
        val realName = rawName.replace("_", " ")
        if (realName == rawName) return event
        if (realName.uppercase() !in SackApi.sackListNames) return event
        return event.eventWithNewMessage(event.message.replace(rawName, realName))
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.gfsSack
}
