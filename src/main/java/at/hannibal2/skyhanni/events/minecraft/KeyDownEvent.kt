package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.InputCode

/** Gets posted when a key is first pressed, use this for taps*/
class KeyDownEvent(val key: InputCode) : SkyHanniEvent() {
    val keyCode: Int get() = key.value
}
