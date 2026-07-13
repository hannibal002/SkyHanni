package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import net.minecraft.network.chat.Component

class ActionBarUpdateEvent(var actionBar: String, var chatComponent: Component) : SkyHanniEvent() {
    fun changeActionBar(newText: String) {
        chatComponent = newText.asComponent()
    }
}
