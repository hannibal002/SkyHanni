package at.hannibal2.skyhanni.events.dungeon

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Event that is fired when a dungeon starts.
 * Meaning after all players are ready, and Mort gives you the map of the dungeon.
 *
 * @param dungeonFloor The floor of the dungeon that is starting.
 */
@PrimaryFunction("onDungeonStart")
class DungeonStartEvent(val dungeonFloor: String) : SkyHanniEvent()
