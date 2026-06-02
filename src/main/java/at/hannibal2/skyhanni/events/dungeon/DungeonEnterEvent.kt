package at.hannibal2.skyhanni.events.dungeon

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonFloor

class DungeonEnterEvent(val dungeonFloor: DungeonFloor) : SkyHanniEvent()
