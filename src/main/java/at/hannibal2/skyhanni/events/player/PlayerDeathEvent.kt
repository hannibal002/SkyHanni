package at.hannibal2.skyhanni.events.player

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.utils.PlayerUtils

abstract class AbstractPlayerDeathEvent(
    override val name: String,
    override val reason: String
) : SkyHanniEvent(), PlayerDeathEvent {
    override val isSelf: Boolean = name == PlayerUtils.getName()
}

sealed interface PlayerDeathEvent {
    val name: String
    val reason: String
    val isSelf: Boolean

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
