package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import net.minecraft.text.Text

class ActionBarUpdateEvent(var actionBar: String, var chatComponent: Text) : SkyHanniEvent() {
    fun changeActionBar(newText: String) {
        chatComponent = newText.asComponent()
    }
}
