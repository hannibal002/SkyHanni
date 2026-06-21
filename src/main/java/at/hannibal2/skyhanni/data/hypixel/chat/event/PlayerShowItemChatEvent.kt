package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

/**
 * Gets fired when a player shows an item or pet in the public chat via /show
 */
object PlayerShowItemChatEvent {

    /**
     * Fired during the read-only phase of the chat processing pipeline.
     * Use this event to read the message or to completely block it from being shown in the chat.
     * Cannot be used to edit or modify the message in any way. For that, see [Modify].
     *
     * @param levelComponent The SkyBlock level.
     * @param action The text indicating the action performed with the item.
     * @param author The message author's name.
     * @param item The chat component representing the shown item.
     * @param message The content of the actual message.
     * @param chatComponent The entire original chat component.
     * @param blockedReason The reason if the message should be blocked. null means not blocked.
     */
    class Allow(
        val levelComponent: ComponentSpan?,
        val action: ComponentSpan,
        author: ComponentSpan,
        val item: ComponentSpan,
        message: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(author, message, chatComponent, blockedReason)

    /**
     * Fired during the modification phase of the chat processing pipeline.
     * Use this specific event to modify the text content or the visual style of the chat component before it shows up on chat.
     * Cannot be used to block the message altogether. Do not use this event for data collection. For both, see [Allow].
     *
     * @param levelComponent The SkyBlock level.
     * @param action The text indicating the action performed with the item.
     * @param author The message author's name.
     * @param item The chat component representing the shown item.
     * @param message The content of the actual message.
     * @param chatComponent The entire original chat component.
     */
    class Modify(
        val levelComponent: ComponentSpan?,
        val action: ComponentSpan,
        author: ComponentSpan,
        val item: ComponentSpan,
        message: ComponentSpan,
        chatComponent: Component,
    ) : AbstractSourcedChatEvent.Modify(author, message, chatComponent)
}
