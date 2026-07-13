package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import at.hannibal2.skyhanni.features.fishing.SeaCreature

@Thread(RENDER)
class SeaCreatureFishEvent(
    val seaCreature: SeaCreature,
    val doubleHook: Boolean,
) : SkyHanniEvent()
