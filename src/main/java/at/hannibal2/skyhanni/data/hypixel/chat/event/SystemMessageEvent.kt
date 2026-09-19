package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.network.chat.Component

/**
 * Gets fired for any chat message not sent by another player or an [NPC][NpcChatEvent].
 */
object SystemMessageEvent {

    /**
     * Fired during the read-only phase of the chat processing pipeline.
     * Use this event to read the message or to completely block it from being shown in the chat.
     * Cannot be used to edit or modify the message in any way. For that, see [Modify].
     *
     * @param message The original message text.
     * @param chatComponent The entire original chat component.
     * @param blockedReason The reason if the message should be blocked. null means not blocked.
     */
    @PrimaryFunction("onSystemMessage")
    open class Allow(
        open val message: String,
        open val chatComponent: Component,
        open var blockedReason: String? = null,
    ) : SkyHanniEvent() {

        /** The plain text message without any color codes. */
        open val cleanMessage: String = chatComponent.string.removeColor()
    }

    /**
     * Fired during the modification phase of the chat processing pipeline.
     * Use this specific event to modify the text content or the visual style of the chat component before it shows up on chat.
     * Cannot be used to block the message altogether. Do not use this event for data collection. For both, see [Allow].
     *
     * @param message The original message text.
     * @param chatComponent The entire original chat component.
     */
    open class Modify(
        open val message: String,
        @set:Deprecated("Use replaceComponent() instead")
        open var chatComponent: Component,
    ) : SkyHanniEvent() {

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
