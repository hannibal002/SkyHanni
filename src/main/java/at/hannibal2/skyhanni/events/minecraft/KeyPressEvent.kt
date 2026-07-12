package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.skyhannimodule.Thread

/** Gets posted each tick it's pressed down*/
@Thread(RENDER)
@PrimaryFunction("onKeyPress")
class KeyPressEvent(val keyCode: Int) : SkyHanniEvent()
