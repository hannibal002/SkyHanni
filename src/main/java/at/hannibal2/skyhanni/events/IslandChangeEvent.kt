package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when the current island state changes.
 *
 * To react to leaving a specific island, check [oldIsland].
 * To react to entering a specific island, check [newIsland].
 * At least one of the two fields will always be [IslandType.NONE].
 *
 * When switching islands, the event fires twice: first with [newIsland] =[IslandType.NONE]
 * (leaving the old island), then with [oldIsland] = [IslandType.NONE] (entering the new one).
 */
@Deprecated("use IslandJoinEvent or IslandLeaveEvent instead")
@PrimaryFunction("onIslandChange")
class IslandChangeEvent(val newIsland: IslandType, val oldIsland: IslandType) : SkyHanniEvent()

@PrimaryFunction("onIslandJoin")
class IslandJoinEvent(val island: IslandType, val previousIsland: IslandType) : SkyHanniEvent()

@PrimaryFunction("onIslandLeave")
class IslandLeaveEvent(val island: IslandType) : SkyHanniEvent()
