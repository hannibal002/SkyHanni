package at.lorenz.mod.chat

import at.lorenz.mod.utils.LorenzLogger
import at.lorenz.mod.events.LorenzChatEvent
import at.lorenz.mod.events.PacketEvent
import at.lorenz.mod.utils.LorenzUtils
import net.minecraft.network.play.server.S02PacketChat
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ChatManager {

    private val loggerAll = LorenzLogger("chat/filter_all")
    private val loggerFiltered = LorenzLogger("chat/filter_blocked")
    private val loggerAllowed = LorenzLogger("chat/filter_allowed")
    private val loggerFilteredTypes = mutableMapOf<String, LorenzLogger>()

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: PacketEvent.ReceiveEvent) {
        val packet = event.packet
        if (packet !is S02PacketChat) return
        val messageComponent = packet.chatComponent

        val message = LorenzUtils.stripVanillaMessage(messageComponent.formattedText)
        if (packet.type.toInt() == 2) {
//            val actionBarEvent = LorenzActionBarEvent(message)
//            actionBarEvent.postAndCatch()
        } else {

            val chatEvent = LorenzChatEvent(message, messageComponent)
            chatEvent.postAndCatch()

            val blockReason = chatEvent.blockedReason.uppercase()
            if (blockReason != "") {
                event.isCanceled = true
                loggerFiltered.log("[$blockReason] $message")
                loggerAll.log("[$blockReason] $message")
                loggerFilteredTypes.getOrPut(blockReason) { LorenzLogger("chat/filter_blocked/$blockReason") }
                    .log(message)
                return
            }

            if (!message.startsWith("§f{\"server\":\"")) {
                loggerAllowed.log(message)
                loggerAll.log("[allowed] $message")
            }

        }
    }
}