package at.hannibal2.skyhanni.events.chat

import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

object AbstractChatEvent {

    open class Allow(
        val messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : SystemMessageEvent.Allow(messageComponent.getText(), chatComponent, blockedReason) {
        @Deprecated(
            "Use cleanMessage unless you really need color codes",
            replaceWith = ReplaceWith("this.cleanMessage")
        )
        override val message = messageComponent.getText().removePrefix("§r")
    }

    open class Modify(
        val messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : SystemMessageEvent.Modify(messageComponent.getText(), chatComponent, blockedReason) {
        @Deprecated(
            "Use cleanMessage unless you really need color codes",
            replaceWith = ReplaceWith("this.cleanMessage")
        )
        override val message = messageComponent.getText().removePrefix("§r")
    }
}
