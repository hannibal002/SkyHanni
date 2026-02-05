package at.hannibal2.skyhanni.data.hypixel.chat.event

import net.minecraft.network.chat.Component

object NpcChatEvent {

    class Allow(
        authorComponent: Component,
        messageComponent: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(authorComponent, messageComponent, chatComponent, blockedReason)

    class Modify(
        authorComponent: Component,
        messageComponent: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(authorComponent, messageComponent, chatComponent, blockedReason)
}
