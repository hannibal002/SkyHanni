package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.utils.system.ModInstance

/**
 * Fired when a message or command is about to be sent from the client to the server.
 * Cancelling this event prevents the message from being sent.
 */
class MessageSendToServerEvent(
    val message: String,
    val splitMessage: List<String>,
    val originatingModContainer: ModInstance?,
) : CancellableSkyHanniEvent() {
    val isAnyCommand by lazy { message.startsWith("/") }

    fun isCommand(commandWithSlash: String): Boolean = splitMessage.takeIf {
        it.isNotEmpty()
    }?.get(0) == commandWithSlash

    fun isCommand(commandsWithSlash: Collection<String>): Boolean =
        splitMessage.takeIf { it.isNotEmpty() }?.get(0) in commandsWithSlash

    fun senderIsSkyhanni(): Boolean = originatingModContainer?.id == "skyhanni"

    fun eventWithNewMessage(message: String): MessageSendToServerEvent =
        MessageSendToServerEvent(message, message.split(" "), this.originatingModContainer)
}
