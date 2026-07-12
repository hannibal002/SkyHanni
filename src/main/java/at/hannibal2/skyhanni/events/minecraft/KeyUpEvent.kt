package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread

/** Counterpart to [KeyDownEvent]*/
@Thread(RENDER)
class KeyUpEvent(val keyCode: Int) : SkyHanniEvent()
