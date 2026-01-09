package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.network.chat.Component

// A SkyHanniChatEvent after filtering all player send events, leaving messages from the game/system.
@PrimaryFunction("onSystemMessage")
open class SystemMessageEvent(
    open val message: String,
    open var chatComponent: Component,
    open var blockedReason: String? = null,
    open var cleanMessage: String = chatComponent.string.removeColor(),
) : SkyHanniEvent() {
    fun replaceComponent(newComponent: Component, reason: String) {
        ChatManager.addReplacementContext(chatComponent, reason)
        chatComponent = newComponent
    }
}
