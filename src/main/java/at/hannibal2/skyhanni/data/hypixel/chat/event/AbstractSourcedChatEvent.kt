package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.events.chat.AbstractChatEvent
import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

/**
 * Contains the base Allow and Modify classes for chat events that include an author.
 * Do not listen to those events directly.
 */
object AbstractSourcedChatEvent {

    /**
     * Fired during the read-only phase of the chat processing pipeline.
     * Use this event to read the message or to completely block it from being shown in the chat.
     * Cannot be used to edit or modify the message in any way. For that, see [Modify].
     *
     * @param authorComponent The chat component representing the author.
     * @param messageComponent The content of the actual message.
     * @param chatComponent The entire original chat component.
     * @param blockedReason The reason if the message should be blocked. null means not blocked.
     */
    open class Allow(
        val authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractChatEvent.Allow(messageComponent, chatComponent, blockedReason) {

        /** The plain text name of the author. */
        val author = authorComponent.getText()
    }

    /**
     * Fired during the modification phase of the chat processing pipeline.
     * Use this specific event to modify the text content or the visual style of the chat component before it shows up on chat.
     * Cannot be used to block the message altogether. Do not use this event for data collection. For both, see [Allow].
     *
     * @param authorComponent The chat component representing the author.
     * @param messageComponent The content of the actual message.
     * @param chatComponent The entire original chat component.
     */
    open class Modify(
        val authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
    ) : AbstractChatEvent.Modify(messageComponent, chatComponent) {

        /** The plain text name of the author. */
        val author = authorComponent.getText()
    }
}
