package at.hannibal2.skyhanni.events.fishing

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.features.fishing.SeaCreature
import at.hannibal2.skyhanni.skyhannimodule.Thread

@Thread(RENDER)
class SeaCreatureFishEvent(
    val seaCreature: SeaCreature,
    val doubleHook: Boolean,
) : SkyHanniEvent()
