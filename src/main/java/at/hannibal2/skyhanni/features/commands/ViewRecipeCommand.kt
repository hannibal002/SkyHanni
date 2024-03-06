package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.NEUItems
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ViewRecipeCommand {

    private val config get() = SkyHanniMod.feature.commands

    @SubscribeEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        if (!config.viewRecipeLowerCase) return
        val message = event.message
        if (!message.startsWith("/viewrecipe ", ignoreCase = true)) return

        if (message == message.uppercase()) return
        event.isCanceled = true
        ChatUtils.sendCommandToServer(message.uppercase().drop(1))
    }

    val list by lazy {
        val list = mutableListOf<String>()
        for ((key, value) in NEUItems.allNeuRepoItems()) {
            if (value.has("recipe")) {
                list.add(key.lowercase())
            }
        }
        list
    }

    fun customTabComplete(command: String): List<String>? {
        if (command == "viewrecipe" && config.tabComplete.viewrecipeItems) {
            return list
        }

        return null
    }
}
