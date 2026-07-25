package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired when an incoming chat message passes the cocoonChatMessage regex from CocoonAPI.
 * Passes the name group as the mobName string
 */
@PrimaryFunction("onHypixelCocoonMessage")
class HypixelCocoonChatMessageEvent(val mobName: String) : SkyHanniEvent()
