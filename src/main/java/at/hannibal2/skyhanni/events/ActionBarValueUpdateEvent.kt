package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.data.ActionBarStatsData

class ActionBarValueUpdateEvent(val updated: ActionBarStatsData) : HanniEvent()
