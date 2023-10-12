package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.apache.commons.lang3.SystemUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

object KeyboardUtils {
    // A mac-only key, represents Windows key on windows (but different key code)
    fun isCommandKeyDown() = OSUtils.isKeyHeld(Keyboard.KEY_LMETA) || OSUtils.isKeyHeld(Keyboard.KEY_RMETA)
    fun isControlKeyDown() = OSUtils.isKeyHeld(Keyboard.KEY_LCONTROL) || OSUtils.isKeyHeld(Keyboard.KEY_RCONTROL)
    fun isShiftKeyDown() = OSUtils.isKeyHeld(Keyboard.KEY_LSHIFT) || OSUtils.isKeyHeld(Keyboard.KEY_RSHIFT)

    fun isPastingKeysDown(): Boolean {
        val modifierHeld = if (SystemUtils.IS_OS_MAC) isCommandKeyDown() else isControlKeyDown()
        return modifierHeld && OSUtils.isKeyHeld(Keyboard.KEY_V)
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (Mouse.getEventButtonState() && Mouse.getEventButton() != -1) {
            val key = Mouse.getEventButton() - 100
            LorenzKeyPressEvent(key).postAndCatch()
            return
        }

        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() != 0) {
            val key = Keyboard.getEventKey()
            LorenzKeyPressEvent(key).postAndCatch()
            return
        }

        // I don't know when this is needed
        if (Keyboard.getEventKey() == 0) {
            LorenzKeyPressEvent(Keyboard.getEventCharacter().code + 256).postAndCatch()
        }
    }
}