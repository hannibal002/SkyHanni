package at.lorenz.mod.chat

import at.lorenz.mod.utils.LorenzLogger
import at.lorenz.mod.events.LorenzChatEvent
import at.lorenz.mod.utils.LorenzUtils
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ChatManager {

    private val loggerAll = LorenzLogger("chat/filter_all")
    private val loggerFiltered = LorenzLogger("chat/filter_blocked")
    private val loggerAllowed = LorenzLogger("chat/filter_allowed")
    private val loggerFilteredTypes = mutableMapOf<String, LorenzLogger>()

    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    fun onChatPacket(event: ClientChatReceivedEvent) {
        val messageComponent = event.message

        val message = LorenzUtils.stripVanillaMessage(messageComponent.formattedText)
        if (event.type.toInt() == 2) {
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