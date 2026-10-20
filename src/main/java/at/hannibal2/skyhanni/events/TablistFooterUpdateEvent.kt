package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.network.chat.Component

/**
 * Fired when the tab list footer changes to a non-empty value.
 *
 * @param footer The new footer [Component].
 */
@PrimaryFunction("onTabListFooterUpdate")
class TablistFooterUpdateEvent(val footer: List<Component>) : SkyHanniEvent()
