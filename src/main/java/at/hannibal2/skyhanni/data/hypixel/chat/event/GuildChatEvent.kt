package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

/**
 * Gets fired when any player sends a message in the guild chat on Hypixel.
 * This explicitly excludes any other message channel like all chat or party chat.
 */
object GuildChatEvent {

    /**
     * Fired during the read-only phase of the chat processing pipeline.
     * Use this event to read the message or to completely block it from being shown in the chat.
     * Cannot be used to edit or modify the message in any way. For that, see [Modify].
     *
     * @param author The message author's name.
     * @param message The content of the actual message.
     * @param guildRank The rank tag of the player within the guild.
     * @param chatComponent The entire original chat component.
     * @param blockedReason The reason if the message should be blocked. null means not blocked.
     */
    class Allow(
        author: ComponentSpan,
        message: ComponentSpan,
        val guildRank: ComponentSpan?,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(author, message, chatComponent, blockedReason)

    /**
     * Fired during the modification phase of the chat processing pipeline.
     * Use this specific event to modify the text content or the visual style of the chat component before it shows up on chat.
     * Cannot be used to block the message altogether. Do not use this event for data collection. For both, see [Allow].
     *
     * @param author The message author's name.
     * @param message The content of the actual message.
     * @param guildRank The rank tag of the player within the guild.
     * @param chatComponent The entire original chat component.
     */
    class Modify(
        author: ComponentSpan,
        message: ComponentSpan,
        val guildRank: ComponentSpan?,
        chatComponent: Component,
    ) : AbstractSourcedChatEvent.Modify(author, message, chatComponent)
}
