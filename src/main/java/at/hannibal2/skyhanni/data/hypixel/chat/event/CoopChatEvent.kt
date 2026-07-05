package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

/**
 * Gets fired when any player sends a message in the coop chat on Hypixel.
 * This explicitly excludes any other message channel like all chat or party chat.
 */
object CoopChatEvent {

    /**
     * Fired during the read-only phase of the chat processing pipeline.
     * Use this event to read the message or to completely block it from being shown in the chat.
     * Cannot be used to edit or modify the message in any way. For that, see [Modify].
     *
     * @param authorComponent The message author's name.
     * @param messageComponent The content of the actual message.
     * @param chatComponent The entire original chat component.
     * @param blockedReason The reason if the message should be blocked. null means not blocked.
     */
    class Allow(
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(authorComponent, messageComponent, chatComponent, blockedReason)

    /**
     * Fired during the modification phase of the chat processing pipeline.
     * Use this specific event to modify the text content or the visual style of the chat component before it shows up on chat.
     * Cannot be used to block the message altogether. Do not use this event for data collection. For both, see [Allow].
     *
     * @param authorComponent The message author's name.
     * @param messageComponent The content of the actual message.
     * @param chatComponent The entire original chat component.
     */
    class Modify(
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
    ) : AbstractSourcedChatEvent.Modify(authorComponent, messageComponent, chatComponent)
}
