package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.events.chat.AbstractChatEvent
import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

object AbstractSourcedChatEvent {

    open class Allow(
        val authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractChatEvent.Allow(messageComponent, chatComponent, blockedReason) {
        val author = authorComponent.getText()
    }

    open class Modify(
        val authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractChatEvent.Modify(messageComponent, chatComponent, blockedReason) {
        val author = authorComponent.getText()
    }
}
