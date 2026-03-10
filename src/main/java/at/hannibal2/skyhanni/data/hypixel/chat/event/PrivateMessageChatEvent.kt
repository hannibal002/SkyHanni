package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

object PrivateMessageChatEvent {

    class Allow(
        val direction: String?,
        author: ComponentSpan,
        message: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(author, message, chatComponent, blockedReason)

    class Modify(
        val direction: String?,
        author: ComponentSpan,
        message: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(author, message, chatComponent, blockedReason)
}
