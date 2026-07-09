package at.hannibal2.skyhanni.events.skyblock

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.AreaType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction


/**
 * Event for detecting area changes in Skyblock.
 * This event is fired when the player changes areas in Skyblock.
 * The area is determined by both the graph and the scoreboard.
 */
@PrimaryFunction("onAreaChange")
open class AreaChangeEvent(val area: AreaType, val previousArea: AreaType?) : SkyHanniEvent() {
    val isNewArea: Boolean get() = area != previousArea
}

/**
 * For detecting area changes in the scoreboard.
 * Only use this if you must specifically use the scoreboard area.
 */
@PrimaryFunction("onScoreboardAreaChange")
class ScoreboardAreaChangeEvent(val area: String, val previousArea: String?) : SkyHanniEvent()

/**
 * For detecting area changes via the graph.
 * Only use this if you must specifically use the graph area.
 */
@PrimaryFunction("onGraphAreaChange")
class GraphAreaChangeEvent(val area: String, val previousArea: String?, val onlyInternal: Boolean) : SkyHanniEvent()
