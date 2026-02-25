package at.hannibal2.skyhanni.events.player

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent

class PlayerDeathEvent(val name: String, val reason: String, val chatEvent: SkyHanniChatEvent.Allow) : SkyHanniEvent()
