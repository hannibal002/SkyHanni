package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

object PrivateMessageChatEvent {

    class Allow(
        val direction: String?,
        author: ComponentSpan,
        privateMessageContents: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(author, privateMessageContents, chatComponent, blockedReason)

    class Modify(
        val direction: String?,
        author: ComponentSpan,
        privateMessageContents: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(author, privateMessageContents, chatComponent, blockedReason)
}
