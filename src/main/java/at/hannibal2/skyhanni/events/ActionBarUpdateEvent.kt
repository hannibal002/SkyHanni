package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import net.minecraft.util.IChatComponent

class ActionBarUpdateEvent(var actionBar: String, var chatComponent: IChatComponent) : SkyHanniEvent() {
    fun changeActionBar(newText: String) {
        chatComponent = newText.asComponent()
    }
}
