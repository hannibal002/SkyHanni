package at.hannibal2.skyhanni.data.hypixel.chat.event

import net.minecraft.network.chat.Component

object PlayerShowItemChatEvent {

    class Allow(
        val levelComponent: Component?,
        val action: Component,
        author: Component,
        val item: Component,
        message: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(author, message, chatComponent, blockedReason)

    class Modify(
        val levelComponent: Component?,
        val action: Component,
        author: Component,
        val item: Component,
        message: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(author, message, chatComponent, blockedReason)
}
