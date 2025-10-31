package at.hannibal2.hanni.events.skyblock

import at.hannibal2.hanni.api.event.HanniEvent

// Detect area changes by looking at the scoreboard.
class ScoreboardAreaChangeEvent(val area: String, val previousArea: String?) : HanniEvent()
class GraphAreaChangeEvent(val area: String, val previousArea: String?, val onlyInternal: Boolean) : HanniEvent()
