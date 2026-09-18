package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.events.chat.AbstractChatEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.ComponentSpan
import net.minecraft.network.chat.Component

/**
 * Gets fired for any chat message not sent by another player or an [NPC][NpcChatEvent].
 *
 * System messages are the fallback chat source for messages that aren't
 * handled by a more specific chat event.
 */
object SystemMessageEvent {

    /**
     * Fired during the read-only phase of the chat processing pipeline.
     *
     * Use this event to read the message or to completely block it from being
     * shown in the chat. Cannot be used to edit or modify the message in any
     * way. For that, see [Modify].
     */
    @PrimaryFunction("onSystemMessage")
    open class Allow(
        messageComponent: ComponentSpan,
        chatComponent: Component,
        blockedReason: String? = null,
    ) : AbstractChatEvent.Allow(
        messageComponent,
        chatComponent,
        blockedReason,
    )

    /**
     * Fired during the modification phase of the chat processing pipeline.
     *
     * Use this event to modify the text content or visual style of the chat
     * component before it shows up on chat. Cannot be used to block the message
     * altogether. Do not use this event for data collection. For both, see [Allow].
     */
    open class Modify(
        messageComponent: ComponentSpan,
        chatComponent: Component,
    ) : AbstractChatEvent.Modify(
        messageComponent,
        chatComponent,
    )
}
