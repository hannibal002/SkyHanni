package at.hannibal2.skyhanni.data.hypixel.chat.event

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
//#if TODO
import at.hannibal2.skyhanni.data.ChatManager
//#endif
import net.minecraft.util.IChatComponent

// todo needs 1.21 impl
// A SkyHanniChatEvent after filtering all player send events, leaving messages from the game/system.
open class SystemMessageEvent(
    open val message: String,
    open var chatComponent: IChatComponent,
    open var blockedReason: String? = null,
) : SkyHanniEvent() {
    fun replaceComponent(newComponent: IChatComponent, reason: String) {
        //#if TODO
        ChatManager.addReplacementContext(chatComponent, reason)
        //#endif
        chatComponent = newComponent
    }
}
