package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.InputCode

/** Gets posted each tick it's pressed down*/
@PrimaryFunction("onKeyPress")
class KeyPressEvent(val keyCode: InputCode) : SkyHanniEvent()
