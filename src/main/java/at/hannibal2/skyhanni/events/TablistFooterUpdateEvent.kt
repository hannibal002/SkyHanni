package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

@Deprecated("use the component version", ReplaceWith("TablistFooterUpdateComponentEvent"))
class TablistFooterUpdateEvent(val footer: String) : SkyHanniEvent()
