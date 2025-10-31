package at.hannibal2.hanni.events.hoppity

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.features.event.hoppity.HoppityEggType

class EggFoundEvent(
    val type: HoppityEggType,
    val slotIndex: Int? = null,
    val note: String? = null,
    val chatEvent: HanniChatEvent? = null
) : HanniEvent()
