package at.hannibal2.hanni.events.fishing

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.fishing.SeaCreature

class SeaCreatureFishEvent(
    val seaCreature: SeaCreature,
    val chatEvent: HanniChatEvent,
    val doubleHook: Boolean,
) : HanniEvent()
