package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.chat.TextHelper
import net.minecraft.network.chat.Component

/**
 * Fired when the tab list footer changes to a non-empty value.
 *
 * @param footer The new footer [Component].
 */
@PrimaryFunction("onTabListFooterUpdate")
class TablistFooterUpdateEvent(val footer: Component) : SkyHanniEvent() {

    val footerList: List<Component> by lazy { TextHelper.split(footer, "\n") ?: listOf(footer) }
}
