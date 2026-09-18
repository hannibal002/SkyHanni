package at.hannibal2.skyhanni.events.chat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.utils.ComponentSpan
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.network.chat.Component

object AbstractChatEvent {

    // TODO docs missing
    open class Allow(
        val messageComponent: ComponentSpan,
        open val chatComponent: Component,
        open var blockedReason: String? = null,
    ) : SkyHanniEvent() {

        @Deprecated(
            "Use cleanMessage unless you really need color codes",
            replaceWith = ReplaceWith("this.cleanMessage")
        )
        open val message = messageComponent.getText().removePrefix("§r")
        open val cleanMessage: String = chatComponent.string.removeColor()
    }

    // TODO docs missing
    open class Modify(
        val messageComponent: ComponentSpan,
        open var chatComponent: Component,
    ) : SkyHanniEvent() {

        @Deprecated(
            "Use cleanMessage unless you really need color codes",
            replaceWith = ReplaceWith("this.cleanMessage")
        )
        open val message = messageComponent.getText().removePrefix("§r")

        open val cleanMessage: String
            get() = chatComponent.string.removeColor()

        fun replaceComponent(newComponent: Component, reason: String) {
            ChatManager.addReplacementContext(chatComponent, reason)
            @Suppress("DEPRECATION")
            chatComponent = newComponent
        }
    }
}
