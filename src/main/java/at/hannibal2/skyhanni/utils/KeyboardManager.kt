package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.LorenzKeyPressEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import io.github.moulberry.moulconfig.internal.KeybindHelper
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.apache.commons.lang3.SystemUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

object KeyboardManager {
    private var lastClickedMouseButton = -1

    // A mac-only key, represents Windows key on windows (but different key code)
    fun isCommandKeyDown() = Keyboard.KEY_LMETA.isKeyHeld() || Keyboard.KEY_RMETA.isKeyHeld()
    fun isControlKeyDown() = Keyboard.KEY_LCONTROL.isKeyHeld() || Keyboard.KEY_RCONTROL.isKeyHeld()
    fun isShiftKeyDown() = Keyboard.KEY_LSHIFT.isKeyHeld() || Keyboard.KEY_RSHIFT.isKeyHeld()

    fun isPastingKeysDown(): Boolean {
        val modifierHeld = if (SystemUtils.IS_OS_MAC) isCommandKeyDown() else isControlKeyDown()
        return modifierHeld && Keyboard.KEY_V.isKeyHeld()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (Mouse.getEventButtonState() && Mouse.getEventButton() != -1) {
            val key = Mouse.getEventButton() - 100
            LorenzKeyPressEvent(key).postAndCatch()
            lastClickedMouseButton = key
            return
        }

        if (Keyboard.getEventKeyState() && Keyboard.getEventKey() != 0) {
            val key = Keyboard.getEventKey()
            LorenzKeyPressEvent(key).postAndCatch()
            lastClickedMouseButton = -1
            return
        }

        if (Mouse.getEventButton() == -1 && lastClickedMouseButton != -1) {
            if (lastClickedMouseButton.isKeyHeld()) {
                LorenzKeyPressEvent(lastClickedMouseButton).postAndCatch()
                println("still holding")
                return
            }
            lastClickedMouseButton = -1
        }

        // I don't know when this is needed
        if (Keyboard.getEventKey() == 0) {
            LorenzKeyPressEvent(Keyboard.getEventCharacter().code + 256).postAndCatch()
        }
    }

    fun KeyBinding.isActive(): Boolean {
        if (!Keyboard.isCreated()) return false
        try {
            if (keyCode.isKeyHeld()) return true
        } catch (e: IndexOutOfBoundsException) {
            println("KeyBinding isActive caused an IndexOutOfBoundsException with keyCode: $keyCode")
            e.printStackTrace()
            return false
        }
        return this.isKeyDown || this.isPressed
    }

    fun Int.isKeyHeld(): Boolean {
        if (this == 0) return false
        return if (this < 0) {
            Mouse.isButtonDown(this + 100)
        } else {
            KeybindHelper.isKeyDown(this)
        }
    }

    fun getKeyName(keyCode: Int): String = KeybindHelper.getKeyName(keyCode)
}