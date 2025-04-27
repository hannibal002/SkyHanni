package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.events.minecraft.KeyHeldEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.KeyReleaseEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuItems
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

@SkyHanniModule
object KeyStateManager {
    //#if MC < 1.16
    // inventory check is not needed, as forge only send events when not in a gui
    private fun shouldTrigger(): Boolean = !NeuItems.neuHasFocus()

    private fun getSyntheticKeyboardKeyCode(key: Int, char: Char): Int = if (key == 0) char.code + 256 else key

    /**
     * Handles keyboard input events. Only outside of GUIs.
     */
    @SubscribeEvent
    fun onKeyboardEvent(event: InputEvent.KeyInputEvent) {
        if (!shouldTrigger()) return

        var keyCode = Keyboard.getEventKey()
        val keyChar = Keyboard.getEventCharacter()
        val keyState = Keyboard.getEventKeyState()
        val isRepeat = Keyboard.isRepeatEvent()

        keyCode = getSyntheticKeyboardKeyCode(keyCode, keyChar)

        if (keyState) {
            if (isRepeat) {
                println("Key held: $keyCode ($keyChar)")
                KeyHeldEvent(keyCode).post()
            } else {
                println("Key pressed: $keyCode ($keyChar)")
                KeyPressEvent(keyCode).post()
            }
        } else {
            println("Key released: $keyCode ($keyChar), repeat: $isRepeat")
            KeyReleaseEvent(keyCode).post()
        }
    }

    /**
     * Handles mouse input events. Only outside of GUIs.
     */
    @SubscribeEvent
    fun onMouseEvent(event: InputEvent.MouseInputEvent) {
        if (!shouldTrigger()) return

        var button = Mouse.getEventButton()
        if (button == -1) return // No button pressed
        button = button - 100
        val buttonName = Mouse.getButtonName(button)
        val buttonState = Mouse.getEventButtonState()
        val mouseX = Mouse.getEventX()
        val mouseY = Mouse.getEventY()
        val scrollDelta = Mouse.getEventDWheel()

        if (buttonState) {
            println("Mouse button pressed: $button, name: $buttonName, position: ($mouseX, $mouseY), scroll delta: $scrollDelta")
            KeyPressEvent(button).post()

        } else {
            println("Mouse button released: $button, name: $buttonName, position: ($mouseX, $mouseY), scroll delta: $scrollDelta")
            KeyReleaseEvent(button).post()
        }
    }
    //#else
    //$$ // todo use fabric event or whatnot
    //#endif
}
