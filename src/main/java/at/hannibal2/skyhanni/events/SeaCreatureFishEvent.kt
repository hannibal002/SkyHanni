package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.features.fishing.SeaCreature

class SeaCreatureFishEvent(
    val seaCreature: SeaCreature,
    val chatEvent: LorenzChatEvent,
    val doubleHook: Boolean
) : LorenzEvent()