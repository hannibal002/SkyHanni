package at.hannibal2.hanni.events.player

import at.hannibal2.hanni.api.event.HanniEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent

/**
 * When the player "you" dies in the game. does not fire when other players die.
 */
class PlayerDeathEvent(val name: String, val reason: String, val chatEvent: HanniChatEvent) : HanniEvent()
