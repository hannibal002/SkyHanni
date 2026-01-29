package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.ChatManager
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.network.chat.Component

// A SkyHanniChatEvent after filtering all player send events, leaving messages from the game/system.
object SystemMessageEvent {

    @PrimaryFunction("onSystemMessage")
    open class Allow(
        open val message: String,
        open val chatComponent: Component,
        open var blockedReason: String? = null,
        open val cleanMessage: String = chatComponent.string.removeColor(),
    ) : SkyHanniEvent()

    open class Modify(
        open val message: String,
        @set:Deprecated("Use replaceComponent() instead")
        open var chatComponent: Component,
        open val blockedReason: String? = null,
        open val cleanMessage: String = chatComponent.string.removeColor(),
    ) : SkyHanniEvent() {
        fun replaceComponent(newComponent: Component, reason: String) {
            ChatManager.addReplacementContext(chatComponent, reason)
            @Suppress("DEPRECATION")
            chatComponent = newComponent
        }
    }
}
