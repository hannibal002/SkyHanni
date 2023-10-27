package at.hannibal2.skyhanni.features.commands

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUItems
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ViewRecipeCommand {
    private val config get() = SkyHanniMod.feature.commands

    @SubscribeEvent
    fun onSendPacket(event: PacketEvent.SendEvent) {
        if (!config.viewRecipeLowerCase) return
        val packet = event.packet
        if (packet is C01PacketChatMessage) {
            val message = packet.message
            if (message == message.uppercase()) return
            if (message.startsWith("/viewrecipe ", ignoreCase = true)) {
                event.isCanceled = true
                LorenzUtils.sendMessageToServer(message.uppercase())
            }
        }
    }

    val list by lazy {
        val list = mutableListOf<String>()
        for ((key, value) in NEUItems.manager.itemInformation) {
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
