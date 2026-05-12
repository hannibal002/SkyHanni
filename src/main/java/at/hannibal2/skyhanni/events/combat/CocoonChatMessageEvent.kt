package at.hannibal2.skyhanni.events.combat

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

@PrimaryFunction("onCocoonChatMessage")
class CocoonChatMessageEvent(val mobName: String): SkyHanniEvent()
