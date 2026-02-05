package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.events.chat.AbstractChatEvent
import net.minecraft.network.chat.Component

object AbstractSourcedChatEvent {

    open class Allow(
        val authorComponent: Component,
        messageComponent: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractChatEvent.Allow(messageComponent, chatComponent, blockedReason) {
        val author = authorComponent.string
    }

    open class Modify(
        val authorComponent: Component,
        messageComponent: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractChatEvent.Modify(messageComponent, chatComponent, blockedReason) {
        val author = authorComponent.string
    }
}
