package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.utils.ComponentSpan
import at.hannibal2.skyhanni.utils.compat.toChatFormatting
import net.minecraft.network.chat.Component

/**
 * Gets fired when any player sends a message in the public chat on Hypixel.
 * This explicitly excludes specialized channels like party chat or guild chat.
 */
object PlayerAllChatEvent {

    /**
     * Fired during the read-only phase of the chat processing pipeline.
     * Use this event to read the message or to completely block it from being shown in the chat.
     * Cannot be used to edit or modify the message in any way. For that, see [Modify].
     *
     * @param levelComponent The SkyBlock level.
     * @param privateIslandRank The rank prefix displayed when the player is on a private island.
     * @param privateIslandGuest The guest indicator tag on private islands.
     * @param chatColor The color code string applied to the original chat message.
     * @param authorComponent The message author's name.
     * @param messageComponent The content of the actual message.
     * @param chatComponent The entire original chat component.
     * @param blockedReason The reason if the message should be blocked. null means not blocked.
     */
    class Allow(
        val levelComponent: ComponentSpan?,
        val privateIslandRank: ComponentSpan?,
        val privateIslandGuest: ComponentSpan?,
        val chatColor: String,
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractSourcedChatEvent.Allow(authorComponent, messageComponent, chatComponent, blockedReason) {

        /** The color code of the SkyBlock level of the player who sent the message */
        val levelColor = levelComponent?.sampleStyleAtStart()?.color?.toChatFormatting()

        /** The SkyBlock level of the player who sent the message */
        val level = levelComponent?.getText()?.toInt()

        /** Is the player who sent the message currently visiting someone else's private island? */
        val isAGuest get() = privateIslandGuest != null
    }

    /**
     * Fired during the modification phase of the chat processing pipeline.
     * Use this specific event to modify the text content or the visual style of the chat component before it shows up on chat.
     * Cannot be used to block the message altogether. Do not use this event for data collection. For both, see [Allow].
     *
     * @param levelComponent The SkyBlock level.
     * @param privateIslandRank The rank prefix displayed when the player is on a private island.
     * @param privateIslandGuest The guest indicator tag on private islands.
     * @param chatColor The color code string applied to the original chat message.
     * @param authorComponent The message author's name.
     * @param messageComponent The content of the actual message.
     * @param chatComponent The entire original chat component.
     */
    class Modify(
        val levelComponent: ComponentSpan?,
        val privateIslandRank: ComponentSpan?,
        val privateIslandGuest: ComponentSpan?,
        val chatColor: String,
        authorComponent: ComponentSpan,
        messageComponent: ComponentSpan,
        chatComponent: Component,
    ) : AbstractSourcedChatEvent.Modify(authorComponent, messageComponent, chatComponent) {

        /** The color code of the SkyBlock level of the player who sent the message */
        val levelColor = levelComponent?.sampleStyleAtStart()?.color?.toChatFormatting()

        /** The SkyBlock level of the player who sent the message */
        val level = levelComponent?.getText()?.toInt()

        /** Is the player who sent the message currently visiting someone else's private island? */
        val isAGuest get() = privateIslandGuest != null
    }
}
