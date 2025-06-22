package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.KeyUpEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.settings.KeyBinding
import org.apache.commons.lang3.SystemUtils
import org.lwjgl.input.Keyboard
import kotlin.time.Duration.Companion.milliseconds
//#if MC < 1.21
import at.hannibal2.skyhanni.data.model.TextInput
import io.github.notenoughupdates.moulconfig.gui.GuiScreenElementWrapper
import org.lwjgl.input.Mouse
//#else
//$$ import net.minecraft.client.util.InputUtil
//#endif

@SkyHanniModule
object KeyboardManager {

    //#if MC < 1.21
    const val LEFT_MOUSE = -100
    const val RIGHT_MOUSE = -99
    const val MIDDLE_MOUSE = -98
    //#else
    //$$ const val LEFT_MOUSE = GLFW.GLFW_MOUSE_BUTTON_LEFT
    //$$ const val RIGHT_MOUSE = GLFW.GLFW_MOUSE_BUTTON_RIGHT
    //$$ const val MIDDLE_MOUSE = GLFW.GLFW_MOUSE_BUTTON_MIDDLE
    //#endif

    private var lastClickedMouseButton = -1

    // A mac-only key, represents Windows key on windows (but different key code)
    private fun isCommandKeyDown() = Keyboard.KEY_LMETA.isKeyHeld() || Keyboard.KEY_RMETA.isKeyHeld()

    // Windows: Alt key Mac: Option key
    fun isMenuKeyDown() = Keyboard.KEY_LMENU.isKeyHeld() || Keyboard.KEY_RMENU.isKeyHeld()

    fun isControlKeyDown() = Keyboard.KEY_LCONTROL.isKeyHeld() || Keyboard.KEY_RCONTROL.isKeyHeld()

    fun isDeleteWordDown() =
        Keyboard.KEY_BACK.isKeyHeld() && if (SystemUtils.IS_OS_MAC) isMenuKeyDown() else isControlKeyDown()

    fun isDeleteLineDown() =
        Keyboard.KEY_BACK.isKeyHeld() && if (SystemUtils.IS_OS_MAC) isCommandKeyDown() else isControlKeyDown() && isShiftKeyDown()

    fun isShiftKeyDown() = Keyboard.KEY_LSHIFT.isKeyHeld() || Keyboard.KEY_RSHIFT.isKeyHeld()

    fun isPastingKeysDown() = isModifierKeyDown() && Keyboard.KEY_V.isKeyHeld()

    fun isCopyingKeysDown() = isModifierKeyDown() && Keyboard.KEY_C.isKeyHeld()

    fun isModifierKeyDown() = if (SystemUtils.IS_OS_MAC) isCommandKeyDown() else isControlKeyDown()

    fun isRightMouseClicked() = RIGHT_MOUSE.isKeyClicked()

    /**
     * TODO make use of this function unnecessary: Try to avoid using `isModifierKeyDown` as the only option,
     * allow the user to set a different option instead and just set the default key to isModifierKeyDown
     */
    fun getModifierKeyName(): String = if (SystemUtils.IS_OS_MAC) "Command" else "Control"

    //#if MC < 1.21
    private data class EventKey(val keyCode: Int, val pressed: Boolean)

    private fun getKeyboardEventKey(): EventKey? {
        val keyCode = getSyntheticKeyboardKeyCode(Keyboard.getEventKey(), Keyboard.getEventCharacter())
        if (keyCode == 0) return null
        val keyState = Keyboard.getEventKeyState()
        return EventKey(keyCode, keyState)
    }

    private fun getMouseEventKey(): EventKey? {
        if (MouseCompat.getEventButton() != -1) {
            val keyCode = MouseCompat.getEventButton() - 100
            lastClickedMouseButton = keyCode
            return EventKey(keyCode, MouseCompat.getEventButtonState())
        }
        if (lastClickedMouseButton != -1 && MouseCompat.getEventButton() == -1) {
            Mouse.poll()
            val originalButton = lastClickedMouseButton + 100
            if (Mouse.isButtonDown(originalButton)) {
                return EventKey(lastClickedMouseButton, true)
            } else {
                lastClickedMouseButton = -1
            }
        }
        return null
    }

    private val pressedKeys = mutableSetOf<Int>()

    private fun getSyntheticKeyboardKeyCode(key: Int, char: Char): Int = if (key == 0) char.code + 256 else key
    //#endif

    //#if MC < 1.16
    @HandleEvent(priority = HandleEvent.LOWEST)
    fun onTick() {
        val currentScreen = Minecraft.getMinecraft().currentScreen
        val isConfigScreen = currentScreen is GuiScreenElementWrapper
        if (isConfigScreen || currentScreen is GuiChat) return

        val keys: List<EventKey> = buildList {
            getKeyboardEventKey()?.let { add(it) }
            getMouseEventKey()?.let { add(it) }
        }

        for (key in keys) {
            if (key.pressed && !pressedKeys.contains(key.keyCode)) {
                postKeyDownEvent(key.keyCode)
                pressedKeys.add(key.keyCode)
            }
        }

        for (keyCode in pressedKeys.toList()) {
            val isDown = if (keyCode < 0) {
                Mouse.isButtonDown(keyCode + 100)
            } else {
                if (keyCode < Keyboard.KEYBOARD_SIZE) {
                    Keyboard.isKeyDown(keyCode)
                } else {
                    false
                }
            }

            if (isDown) {
                postKeyPressEvent(keyCode)
            } else {
                postKeyUpEvent(keyCode)
                pressedKeys.remove(keyCode)
            }
        }
    }
    //#endif
    // on 1.21 we use MixinKeyboard, it provides all of this

    /*
    The delay below is here to make sure the Text input features in graph editor
    and in renderable calls have time to react first, and lock this key press event properly
     */

    // On 1.21 we post these events inside mixins
    //#if MC < 1.21
    private fun postKeyPressEvent(keyCode: Int) {
        DelayedRun.runDelayed(50.milliseconds) {
            if (TextInput.isActive()) return@runDelayed
            KeyPressEvent(keyCode).post()
        }
    }

    private fun postKeyDownEvent(keyCode: Int) {
        DelayedRun.runDelayed(50.milliseconds) {
            if (TextInput.isActive()) return@runDelayed
            KeyDownEvent(keyCode).post()
        }
    }

    private fun postKeyUpEvent(keyCode: Int) {
        DelayedRun.runDelayed(50.milliseconds) {
            if (TextInput.isActive()) return@runDelayed
            KeyUpEvent(keyCode).post()
        }
    }
    //#endif

    fun KeyBinding.isActive(): Boolean {
        //#if MC < 1.16
        if (!Keyboard.isCreated()) return false
        try {
            if (keyCode.isKeyHeld()) return true
        } catch (e: IndexOutOfBoundsException) {
            ErrorManager.logErrorWithData(
                e,
                "Error while checking if a key is pressed.",
                "keyCode" to keyCode,
            )
            return false
        }
        //#endif
        return this.isKeyDown || this.isPressed
    }

    fun Int.isKeyHeld(): Boolean = when {
        //#if MC < 1.16
        this == 0 -> false
        this < 0 -> MouseCompat.isButtonDown(this + 100)
        this >= Keyboard.KEYBOARD_SIZE -> {
            val pressedKey = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
            Keyboard.getEventKeyState() && this == pressedKey
        }

        else -> Keyboard.isKeyDown(this)
        //#else
        //$$ this < -1 -> ErrorManager.skyHanniError("Error while checking if a key is pressed. Keycode is invalid", "keycode" to this)
        //$$ this == -1 -> false
        //$$ this in 0..5 -> MouseCompat.isButtonDown(this)
        //$$ else -> InputUtil.isKeyPressed(MinecraftClient.getInstance().window.handle, this)
        //#endif
    }

    private val lockedKeys = mutableMapOf<Int, Boolean>()

    /**
     * Can only be used once per click, since the function locks itself until the key is no longer held.
     * Do not use in KeyPressEvent, since it won't be unlocked again, use KeyDownEvent instead.
     * */
    fun Int.isKeyClicked(): Boolean = if (this.isKeyHeld()) {
        if (lockedKeys[this] != true) {
            lockedKeys[this] = true
            true
        } else {
            false
        }
    } else {
        lockedKeys[this] = false
        false
    }

    fun getKeyName(keyCode: Int): String = IMinecraft.instance.getKeyName(keyCode)

    object WasdInputMatrix : Iterable<KeyBinding> {
        operator fun contains(keyBinding: KeyBinding) = when (keyBinding) {
            w, a, s, d, up, down -> true
            else -> false
        }

        val w get() = Minecraft.getMinecraft().gameSettings.keyBindForward!!
        val a get() = Minecraft.getMinecraft().gameSettings.keyBindLeft!!
        val s get() = Minecraft.getMinecraft().gameSettings.keyBindBack!!
        val d get() = Minecraft.getMinecraft().gameSettings.keyBindRight!!

        val up get() = Minecraft.getMinecraft().gameSettings.keyBindJump!!
        val down get() = Minecraft.getMinecraft().gameSettings.keyBindSneak!!

        override fun iterator(): Iterator<KeyBinding> =
            object : Iterator<KeyBinding> {

                var current = w
                var finished = false

                override fun hasNext(): Boolean =
                    !finished

                override fun next(): KeyBinding {
                    if (!hasNext()) throw NoSuchElementException()

                    return current.also {
                        current = when (it) {
                            w -> a
                            a -> s
                            s -> d
                            d -> up
                            up -> down
                            else -> {
                                finished = true
                                throw NoSuchElementException()
                            }
                        }
                    }
                }

            }

    }
}
