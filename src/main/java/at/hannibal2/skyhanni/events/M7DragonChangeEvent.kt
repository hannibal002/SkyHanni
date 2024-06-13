package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.dungeon.m7.M7SpawnedStatus
import at.hannibal2.skyhanni.features.dungeon.m7.WitheredDragonInfo

class M7DragonChangeEvent(dragon: WitheredDragonInfo, state: M7SpawnedStatus) : SkyHanniEvent()
