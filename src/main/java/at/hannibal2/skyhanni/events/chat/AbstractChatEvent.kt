package at.hannibal2.skyhanni.events.chat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.utils.ComponentSpan
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.network.chat.Component

/**
 * Contains the base Allow and Modify classes for all chat events.
 * Do not listen to those events directly.
 */
object AbstractChatEvent {

    /**
     * Fired during the read-only phase of the chat processing pipeline.
     * Use this event to read the message or to completely block it from being shown in the chat.
     * Cannot be used to edit or modify the message in any way. For that, see [Modify].
     *
     * @param messageComponent The content of the actual message.
     * @param chatComponent The entire original chat component.
     * @param blockedReason The reason if the message should be blocked. null means not blocked.
     */
    abstract class Allow(
        open val messageComponent: ComponentSpan,
        open val chatComponent: Component,
        open var blockedReason: String? = null,
    ) : SkyHanniEvent() {
        @Deprecated(
            "Use cleanMessage unless you really need color codes",
            replaceWith = ReplaceWith("this.cleanMessage"),
        )
        open val message: String = messageComponent.getText().removePrefix("§r")

        /** The plain text message without any color codes. */
        open val cleanMessage: String = chatComponent.string.removeColor()
    }

    /**
     * Fired during the modification phase of the chat processing pipeline.
     * Use this specific event to modify the text content or the visual style of the chat component before it shows up on chat.
     * Cannot be used to block the message altogether. Do not use this event for data collection. For both, see [Allow].
     *
     * @param messageComponent The content of the actual message.
     * @param chatComponent The entire original chat component.
     */
    abstract class Modify(
        open val messageComponent: ComponentSpan,
        @set:Deprecated("Use replaceComponent() instead")
        open var chatComponent: Component,
    ) : SkyHanniEvent() {
        @Deprecated(
            "Use cleanMessage unless you really need color codes",
            replaceWith = ReplaceWith("this.cleanMessage"),
        )
        open val message: String = messageComponent.getText().removePrefix("§r")

        /** The plain text message without any color codes. */
        open val cleanMessage: String
            get() = chatComponent.string.removeColor()

        fun replaceComponent(newComponent: Component, reason: String) {
            ChatManager.addReplacementContext(chatComponent, reason)
            @Suppress("DEPRECATION")
            chatComponent = newComponent
        }
    }
}
