package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.ChatManager
import net.minecraft.util.IChatComponent

// A SkyHanniChatEvent after filtering all player send events, leaving messages from the game/system.
open class SystemMessageEvent(
    open val message: String,
    open var chatComponent: IChatComponent,
    open var blockedReason: String? = null,
) : SkyHanniEvent() {
    fun replaceComponent(newComponent: IChatComponent, reason: String) {
        ChatManager.addReplacementContext(chatComponent, reason)
        chatComponent = newComponent
    }
}
