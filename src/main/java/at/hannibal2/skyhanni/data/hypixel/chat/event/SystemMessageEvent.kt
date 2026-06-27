package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.events.chat.AbstractChatEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

/**
 * Gets fired for any chat message not sent by another player or an [NPC][NpcChatEvent].
 * Messages are not guaranteed to be sent by the system if the user has unusual settings.
 * Instead, use [SkyHanniChatEvent] and stronger regexes.
 */
@Deprecated(
    "Use SkyHanniChatEvent and a stronger regex to filter out player messages",
    replaceWith = ReplaceWith("SkyHanniChatEvent"),
)
object SystemMessageEvent {

    /**
     * Fired during the read-only phase of the chat processing pipeline.
     * Use this event to read the message or to completely block it from being shown in the chat.
     * Cannot be used to edit or modify the message in any way. For that, see [Modify].
     *
     * @param messageComponent The content of the actual message.
     * @param chatComponent The entire original chat component.
     * @param blockedReason The reason if the message should be blocked. null means not blocked.
     */
    class Allow(
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractChatEvent.Allow(messageComponent, chatComponent, blockedReason)

    /**
     * Fired during the modification phase of the chat processing pipeline.
     * Use this specific event to modify the text content or the visual style of the chat component before it shows up on chat.
     * Cannot be used to block the message altogether. Do not use this event for data collection. For both, see [Allow].
     *
     * @param messageComponent The content of the actual message.
     * @param chatComponent The entire original chat component.
     */
    class Modify(
        messageComponent: ComponentSpan,
        chatComponent: Component,
    ) : AbstractChatEvent.Modify(messageComponent, chatComponent)
}
