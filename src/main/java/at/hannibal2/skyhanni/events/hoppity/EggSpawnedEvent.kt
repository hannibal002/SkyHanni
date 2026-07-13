package at.hannibal2.skyhanni.events.hoppity

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.event.hoppity.HoppityEggType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onEggSpawned")
class EggSpawnedEvent(val eggType: HoppityEggType) : SkyHanniEvent()
