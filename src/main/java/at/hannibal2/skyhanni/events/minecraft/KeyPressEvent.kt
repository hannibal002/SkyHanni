package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.config.ConfigKeybind
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.InputCode

/** Gets posted each tick it's pressed down*/
@PrimaryFunction("onKeyPress")
class KeyPressEvent(val keyCode: Int) : SkyHanniEvent() {
    fun isPressed(keyBindOpen: ConfigKeybind): Boolean {
        return keyCode == keyBindOpen.value
    }
    fun isPressed(keyBindOpen: InputCode): Boolean {
        return keyCode == keyBindOpen.value
    }
}
