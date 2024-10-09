package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI

class DungeonPhaseChangeEvent(val newPhase: DungeonAPI.DungeonPhase) : SkyHanniEvent()
