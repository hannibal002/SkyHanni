package at.hannibal2.hanni.events.dungeon

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.dungeon.DungeonBossApi

class DungeonBossPhaseChangeEvent(val newPhase: DungeonBossApi.DungeonBossPhase) : HanniEvent()
