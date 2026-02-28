package at.hannibal2.skyhanni.events.player

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.utils.PlayerUtils

abstract class AbstractPlayerDeathEvent(
    val name: String,
    val reason: String,
) : SkyHanniEvent() {
    val isSelf: Boolean = name == PlayerUtils.getName()
}

object PlayerDeathEvent {
    class Allow(
        name: String,
        reason: String,
        val chatEvent: SkyHanniChatEvent.Allow,
    ) : AbstractPlayerDeathEvent(name, reason)

    class Modify(
        name: String,
        reason: String,
        val chatEvent: SkyHanniChatEvent.Modify,
    ) : AbstractPlayerDeathEvent(name, reason)
}
