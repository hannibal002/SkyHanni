package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

class ScoreboardUpdateEvent(val scoreboard: List<String>) : SkyHanniEvent()
