package at.hannibal2.skyhanni.events.player

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.utils.PlayerUtils

open class PlayerDeathEvent(val name: String, val reason: String) : SkyHanniEvent() {
    val isSelf: Boolean = name == PlayerUtils.getName()

    class Allow(
        name: String,
        reason: String,
        val chatEvent: SkyHanniChatEvent.Allow,
    ) : PlayerDeathEvent(name, reason)

    class Modify(
        name: String,
        reason: String,
        val chatEvent: SkyHanniChatEvent.Modify,
    ) : PlayerDeathEvent(name, reason)
}
