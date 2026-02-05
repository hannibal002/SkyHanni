package at.hannibal2.skyhanni.events.chat

import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import net.minecraft.network.chat.Component

object AbstractChatEvent {

    open class Allow(
        val messageComponent: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : SystemMessageEvent.Allow(messageComponent.string, chatComponent, blockedReason) {
        @Deprecated(
            "Use cleanMessage unless you really need color codes",
            replaceWith = ReplaceWith("this.cleanMessage")
        )
        override val message = messageComponent.formattedTextCompat().removePrefix("§r")
    }

    open class Modify(
        val messageComponent: Component,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : SystemMessageEvent.Modify(messageComponent.string, chatComponent, blockedReason) {
        @Deprecated(
            "Use cleanMessage unless you really need color codes",
            replaceWith = ReplaceWith("this.cleanMessage")
        )
        override val message = messageComponent.formattedTextCompat().removePrefix("§r")
    }
}
