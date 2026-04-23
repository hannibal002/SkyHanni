package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

object PrivateMessageChatEvent {

    class Allow(
        val direction: Direction,
        author: ComponentSpan,
        message: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(author, message, chatComponent, blockedReason)

    class Modify(
        val direction: Direction,
        author: ComponentSpan,
        message: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(author, message, chatComponent, blockedReason)
}

enum class Direction(val text: String) {
    OUTGOING("To"),
    INCOMING("From"),
    ;

    companion object {
        fun fromString(string: String): Direction = entries.firstOrNull { it.text == string } ?: error("Invalid direction string: $string")
    }
}
