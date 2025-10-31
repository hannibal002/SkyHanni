package at.hannibal2.hanni.events

import at.hannibal2.hanni.api.event.HanniEvent

class RawScoreboardUpdateEvent(val rawScoreboard: List<String>) : HanniEvent()
