package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.dungeon.m7.M7DragonInfo
import at.hannibal2.skyhanni.features.dungeon.m7.M7SpawnedStatus

class M7DragonChangeEvent(dragon: M7DragonInfo, state: M7SpawnedStatus) : SkyHanniEvent()
