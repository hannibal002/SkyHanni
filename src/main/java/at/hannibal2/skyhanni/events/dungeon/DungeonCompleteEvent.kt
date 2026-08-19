package at.hannibal2.skyhanni.events.dungeon

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/**
 * Fired when a dungeon run is completed.
 *
 * Posted when the dungeon completion title appears in chat (e.g., "The Catacombs - Floor VII" or
 * "Master Mode The Catacombs - Floor VII").
 *
 * [floor] holds the current dungeon floor identifier (e.g., "F7", "M7", "E").
 */
class DungeonCompleteEvent(val floor: String) : SkyHanniEvent()
