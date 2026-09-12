package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.config.ConfigKeybind
import at.hannibal2.skyhanni.utils.InputCode

/** Counterpart to [KeyDownEvent]*/
class KeyUpEvent(val keyCode: Int) : SkyHanniEvent() {
    fun isPressed(keyBind: ConfigKeybind): Boolean = keyCode == keyBind.value
    fun isPressed(key: InputCode): Boolean = keyCode == key.value
}
