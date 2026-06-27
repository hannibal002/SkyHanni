package at.hannibal2.skyhanni.events.chat

import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import net.minecraft.network.chat.Component

/**
 * Gets fired for all chat messages received.
 */
object SkyHanniChatEvent {

    /**
     * Fired during the read-only phase of the chat processing pipeline.
     * Use this event to read the message or to completely block it from being shown in the chat.
     * Cannot be used to edit or modify the message in any way. For that, see [Modify].
     *
     * @param message The original message text.
     * @param chatComponent The entire original chat component.
     * @param blockedReason The reason if the message should be blocked. null means not blocked.
     */
    @PrimaryFunction("onChat")
    class Allow(
        message: String,
        chatComponent: Component,
        blockedReason: String? = null,
        var chatLineId: Int = 0,
    ) : AbstractChatEvent.Allow(message.asComponent().intoSpan(), chatComponent, blockedReason)

    /**
     * Fired during the modification phase of the chat processing pipeline.
     * Use this specific event to modify the text content or the visual style of the chat component before it shows up on chat.
     * Cannot be used to block the message altogether. Do not use this event for data collection. For both, see [Allow].
     *
     * @param message The original message text.
     * @param chatComponent The entire original chat component.
     */
    class Modify(
        message: String,
        chatComponent: Component,
    ) : AbstractChatEvent.Modify(message.asComponent().intoSpan(), chatComponent)
}
