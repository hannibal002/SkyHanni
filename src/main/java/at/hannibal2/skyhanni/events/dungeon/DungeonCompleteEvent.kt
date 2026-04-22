package at.hannibal2.skyhanni.events.dungeon

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when a dungeon run is completed.
 *
 * Posted when the dungeon completion title appears in chat (e.g., "The Catacombs - Floor VII" or
 * "Master Mode The Catacombs - Floor VII").
 *
 * [floor] holds the current dungeon floor identifier (e.g., "F7", "M7", "E").
 */
@PrimaryFunction("onDungeonComplete")
class DungeonCompleteEvent(val floor: String) : SkyHanniEvent()
