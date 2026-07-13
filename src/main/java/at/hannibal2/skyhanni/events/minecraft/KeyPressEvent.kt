package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/** Gets posted each tick it's pressed down*/
@PrimaryFunction("onKeyPress")
class KeyPressEvent(val keyCode: Int) : SkyHanniEvent()
