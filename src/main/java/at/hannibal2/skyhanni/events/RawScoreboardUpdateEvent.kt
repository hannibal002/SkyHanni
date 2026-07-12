package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread

@Thread(RENDER)
class RawScoreboardUpdateEvent(val rawScoreboard: List<String>) : SkyHanniEvent()
