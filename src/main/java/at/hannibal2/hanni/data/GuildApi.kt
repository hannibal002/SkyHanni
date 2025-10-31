package at.hannibal2.hanni.data

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.StringUtils.cleanPlayerName

@HanniModule
object GuildApi {

    private var inGuildMessage = false
    private val list = mutableListOf<String>()

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        val message = event.message
        if (message.startsWith("§6Guild Name: ")) {
            inGuildMessage = true
            list.clear()
            return
        }
        if (message.startsWith("§eOffline Members: ")) {
            inGuildMessage = false
            list.clear()
            return
        }
        if (inGuildMessage && message == "§b§m-----------------------------------------------------") {
            inGuildMessage = false
            ProfileStorageData.playerSpecific?.guildMembers?.let {
                it.clear()
                it.addAll(list)
            }
            list.clear()
            return
        }

        if (inGuildMessage) {
            if (message.contains("●")) {
                for (word in message.split("●")) {
                    list.add(word.cleanPlayerName())
                }
            }
        }
    }

    fun isInGuild(name: String) = name in getAllMembers()

    fun getAllMembers() = ProfileStorageData.playerSpecific?.guildMembers.orEmpty()
}
