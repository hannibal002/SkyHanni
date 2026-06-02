package at.hannibal2.skyhanni.events.dungeon

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonFloor
import kotlin.time.Duration

/**
 * gets fired when the dungeon is done. either a death or the final boss is killed.
 *  @param dungeonFloor the floor of the dungeon, e.g. F1
 *  @param time the time it took to close the dungeon, taken from the scoreboard.
 */
class DungeonCompleteEvent(val dungeonFloor: DungeonFloor, @Deprecated("use dungeonFloor") val floor: String, val time: Duration) :
    SkyHanniEvent()
