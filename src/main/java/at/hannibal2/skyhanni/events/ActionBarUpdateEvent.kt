package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import net.minecraft.network.chat.Component

/**
 * Event that is called when the action bar is updated.
 *
 * @param actionBar The action bar text. with color codes.
 * @param chatComponent The raw action bar text as a Component.
 */
@PrimaryFunction("onActionBarUpdate")
class ActionBarUpdateEvent(var actionBar: String, var chatComponent: Component) : SkyHanniEvent() {
    fun changeActionBar(newText: String) {
        chatComponent = newText.asComponent()
    }

    val cleanActionBar = actionBar.removeColor()
}
