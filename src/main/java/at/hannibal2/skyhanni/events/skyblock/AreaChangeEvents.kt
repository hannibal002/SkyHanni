package at.hannibal2.skyhanni.events.skyblock

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread

// Detect area changes by looking at the scoreboard.
@Thread(RENDER)
class ScoreboardAreaChangeEvent(val area: String, val previousArea: String?) : SkyHanniEvent()
@Thread(RENDER)
class GraphAreaChangeEvent(val area: String, val previousArea: String?, val onlyInternal: Boolean) : SkyHanniEvent()
