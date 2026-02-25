package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import net.minecraft.network.chat.HoverEvent

/**
 * This event is used to perform things when a chat component which contains a HoverEvent is hovered.
 *
 * To edit the value of a certain HoverEvent, either the `entity` from `HoverEvent.ShowEntity`,
 * `item` from `HoverEvent.ShowItem`, or `value` from `HoverEvent.ShowText`, separate functionality
 * must be implemented / used.
 *
 * Currently, only functionality to change the `value` of `HoverEvent.ShowText` exists through the
 * use of methods in [GuiChatHook][at.hannibal2.skyhanni.mixins.hooks.GuiChatHook].
 */
class ChatHoverEvent(private val hoverEvent: HoverEvent?) : SkyHanniEvent() {
    fun get(): HoverEvent = hoverEvent ?: ErrorManager.skyHanniError("Hover event from component is missing")
}
