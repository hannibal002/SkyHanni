package at.hannibal2.skyhanni.events.minecraft

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.InputCode

import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction

/**
 * Fired once when a key is pressed down. Use this for taps.
 *
 * Fired on the main client thread from `MixinKeyboardHandler`, at the head of `KeyboardHandler.keyPress`
 * and before Minecraft handles the input. It does not repeat while the key stays held, use [KeyPressEvent]
 * for that. [KeyUpEvent] is the counterpart.
 *
 * Not fired while no player exists, for an unknown key code, or while the REI search bar has focus.
 *
 * @param keyCode the GLFW key code of the pressed key
 */
@PrimaryFunction("onKeyDown")
class KeyDownEvent(val key: InputCode) : SkyHanniEvent() {
    val keyCode: Int get() = key.value
}
