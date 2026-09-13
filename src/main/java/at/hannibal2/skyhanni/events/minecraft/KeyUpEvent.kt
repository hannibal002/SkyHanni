package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.InputCode

/** Counterpart to [KeyDownEvent]*/
class KeyUpEvent(val key: InputCode) : SkyHanniEvent() {
    val keyCode: Int get() = key.value
}
