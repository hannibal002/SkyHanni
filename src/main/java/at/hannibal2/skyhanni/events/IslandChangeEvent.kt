package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction


// TODO to support checking "switched from island X to island Y" where both are specific islands,
//  expand this event with a "last valid island" field that holds the last non-[IslandType.NONE] island.
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
@PrimaryFunction("onIslandChange")
class IslandChangeEvent(val newIsland: IslandType, val oldIsland: IslandType) : SkyHanniEvent()
