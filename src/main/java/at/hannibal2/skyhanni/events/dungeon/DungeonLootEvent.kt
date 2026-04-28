package at.hannibal2.skyhanni.events.dungeon

import at.hannibal2.skyhanni.api.event.SkyHanniEvent

/**
 * Gets fired when the player opens a reward chest in Dungeons or in Croesus
 */
class DungeonLootEvent(
    val cost: Int,
    val usedKey: Boolean,
    val chestType: String,
    val loot: List<Pair<String, Int>>,
) : SkyHanniEvent()
