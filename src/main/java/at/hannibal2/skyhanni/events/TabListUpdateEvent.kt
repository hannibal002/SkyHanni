package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent

class TabListUpdateEvent(val tabList: List<String>) : HanniEvent()
