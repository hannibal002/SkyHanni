package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

object NpcChatEvent {

    class Allow(
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(authorComponent, messageComponent, chatComponent, blockedReason)

    class Modify(
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Modify(authorComponent, messageComponent, chatComponent, blockedReason)
}
