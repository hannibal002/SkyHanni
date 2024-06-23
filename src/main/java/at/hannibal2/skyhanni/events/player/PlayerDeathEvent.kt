package at.hannibal2.skyhanni.events.player

import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzEvent

/**
 * When the player "you" dies in the game. does not fire when other players die.
 */
class PlayerDeathEvent(val name: String, val reason: String, val chatEvent: LorenzChatEvent) : LorenzEvent()
