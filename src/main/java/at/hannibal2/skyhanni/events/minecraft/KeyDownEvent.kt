package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.config.ConfigKeybind
import at.hannibal2.skyhanni.utils.InputCode

/** Gets posted when a key is first pressed, use this for taps*/
class KeyDownEvent(val keyCode: Int) : SkyHanniEvent() {
    fun isPressed(keyBind: ConfigKeybind): Boolean = keyCode == keyBind.value
    fun isPressed(key: InputCode): Boolean = keyCode == key.value
}
