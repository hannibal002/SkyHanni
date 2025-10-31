package at.hannibal2.hanni.data.hypixel.chat.event

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.ChatManager
import at.hannibal2.hanni.hannimodule.PrimaryFunction
import net.minecraft.util.IChatComponent

// A HanniChatEvent after filtering all player send events, leaving messages from the game/system.
@PrimaryFunction("onSystemMessage")
open class SystemMessageEvent(
    open val message: String,
    open var chatComponent: IChatComponent,
    open var blockedReason: String? = null,
) : HanniEvent() {
    fun replaceComponent(newComponent: IChatComponent, reason: String) {
        ChatManager.addReplacementContext(chatComponent, reason)
        chatComponent = newComponent
    }
}
