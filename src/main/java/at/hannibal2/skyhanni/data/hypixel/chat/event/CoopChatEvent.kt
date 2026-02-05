package at.hannibal2.skyhanni.data.hypixel.chat.event

import net.minecraft.network.chat.Component

object CoopChatEvent {

    class Allow(
        authorComponent: Component,
        messageComponent: Component,
        fullComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(authorComponent, messageComponent, fullComponent, blockedReason)

    class Modify(
        authorComponent: Component,
        messageComponent: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(authorComponent, messageComponent, chatComponent, blockedReason)
}
