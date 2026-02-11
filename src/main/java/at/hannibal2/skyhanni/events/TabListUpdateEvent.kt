package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

@Deprecated("use the component version", ReplaceWith("TabListUpdateComponentEvent"))
class TabListUpdateEvent(val tabList: List<String>) : SkyHanniEvent()
