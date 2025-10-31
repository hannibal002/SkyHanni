package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.utils.chat.TextHelper.asComponent
import net.minecraft.util.IChatComponent

class ActionBarUpdateEvent(var actionBar: String, var chatComponent: IChatComponent) : HanniEvent() {
    fun changeActionBar(newText: String) {
        chatComponent = newText.asComponent()
    }
}
