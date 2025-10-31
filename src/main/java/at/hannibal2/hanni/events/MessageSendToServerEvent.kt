package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.CancellableHanniEvent
import at.hannibal2.hanni.utils.system.ModInstance

class MessageSendToServerEvent(
    val message: String,
    val splitMessage: List<String>,
    val originatingModContainer: ModInstance?
) : CancellableHanniEvent() {
    val isCommand by lazy { message.startsWith("/") }
}
