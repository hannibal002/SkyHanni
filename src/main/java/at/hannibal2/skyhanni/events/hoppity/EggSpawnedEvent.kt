package at.hannibal2.hanni.events.hoppity

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.features.event.hoppity.HoppityEggType
import at.hannibal2.hanni.hannimodule.PrimaryFunction

@PrimaryFunction("onEggSpawned")
class EggSpawnedEvent(val eggType: HoppityEggType) : HanniEvent()
