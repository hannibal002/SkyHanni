package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.inventory.AttemptedInventoryCloseEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.minecraft.KeyUpEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.KeyMapping
import org.apache.commons.lang3.SystemUtils
import org.lwjgl.glfw.GLFW
import kotlin.time.Duration.Companion.milliseconds
//#if MC < 1.21
//$$ import at.hannibal2.skyhanni.data.model.TextInput
//$$ import io.github.notenoughupdates.moulconfig.gui.GuiScreenElementWrapper
//$$ import org.lwjgl.input.Mouse
//#else
import com.mojang.blaze3d.platform.InputConstants
//#endif
//#if MC > 1.21.8
//$$ import net.minecraft.client.input.KeyInput
//#endif

@SkyHanniModule
object KeyboardManager {

    //#if MC < 1.21
    //$$ const val LEFT_MOUSE = -100
    //$$ const val RIGHT_MOUSE = -99
    //$$ const val MIDDLE_MOUSE = -98
    //#else
    const val LEFT_MOUSE = GLFW.GLFW_MOUSE_BUTTON_LEFT
    const val RIGHT_MOUSE = GLFW.GLFW_MOUSE_BUTTON_RIGHT
    const val MIDDLE_MOUSE = GLFW.GLFW_MOUSE_BUTTON_MIDDLE
    //#endif

    private var lastClickedMouseButton = -1

    // A mac-only key, represents Windows key on windows (but different key code)
    private fun isCommandKeyDown() = GLFW.GLFW_KEY_LEFT_SUPER.isKeyHeld() || GLFW.GLFW_KEY_RIGHT_SUPER.isKeyHeld()

    // Windows: Alt key Mac: Option key
    fun isMenuKeyDown() = GLFW.GLFW_KEY_LEFT_ALT.isKeyHeld() || GLFW.GLFW_KEY_RIGHT_ALT.isKeyHeld()

    fun isControlKeyDown() = GLFW.GLFW_KEY_LEFT_CONTROL.isKeyHeld() || GLFW.GLFW_KEY_RIGHT_CONTROL.isKeyHeld()

    fun isDeleteWordDown() =
        GLFW.GLFW_KEY_BACKSPACE.isKeyHeld() && if (SystemUtils.IS_OS_MAC) isMenuKeyDown() else isControlKeyDown()

    fun isDeleteLineDown() =
        GLFW.GLFW_KEY_BACKSPACE.isKeyHeld() && if (SystemUtils.IS_OS_MAC) isCommandKeyDown() else isControlKeyDown() && isShiftKeyDown()

    fun isShiftKeyDown() = GLFW.GLFW_KEY_LEFT_SHIFT.isKeyHeld() || GLFW.GLFW_KEY_RIGHT_SHIFT.isKeyHeld()

    fun isPastingKeysDown() = isModifierKeyDown() && GLFW.GLFW_KEY_V.isKeyHeld()

    fun isCopyingKeysDown() = isModifierKeyDown() && GLFW.GLFW_KEY_C.isKeyHeld()

    fun isModifierKeyDown() = if (SystemUtils.IS_OS_MAC) isCommandKeyDown() else isControlKeyDown()

    @JvmStatic
    fun checkIsInventoryClosure(keycode: Int): Boolean {
        // Holding shift bypasses closure checks
        if (isShiftKeyDown()) return false

        val isClose =
            //#if MC < 1.21
            //$$ keycode == MinecraftClient.getInstance().options.keyInventory.boundKey.getCode() || keycode == GLFW.GLFW_KEY_ESCAPE
        //#elseif MC < 1.21.9
        Minecraft.getInstance().options.keyInventory.matches(keycode, keycode) || keycode == GLFW.GLFW_KEY_ESCAPE
        //#else
        //$$ MinecraftClient.getInstance().options.inventoryKey.matchesKey(KeyInput(keycode, keycode, 0)) || keycode == GLFW.GLFW_KEY_ESCAPE
        //#endif

        if (!isClose) return false
        return AttemptedInventoryCloseEvent().post()
    }

    /**
     * TODO make use of this function unnecessary: Try to avoid using `isModifierKeyDown` as the only option,
     * allow the user to set a different option instead and just set the default key to isModifierKeyDown
     */
    fun getModifierKeyName(): String = if (SystemUtils.IS_OS_MAC) "Command" else "Control"

    //#if MC < 1.21
    //$$ private data class EventKey(val keyCode: Int, val pressed: Boolean)
    //$$
    //$$ private fun getKeyboardEventKey(): EventKey? {
    //$$     val keyCode = getSyntheticKeyboardKeyCode(GLFW.getEventKey(), GLFW.getEventCharacter())
    //$$     if (keyCode == 0) return null
    //$$     val keyState = GLFW.getEventKeyState()
    //$$     return EventKey(keyCode, keyState)
    //$$ }
    //$$
    //$$ private fun getMouseEventKey(): EventKey? {
    //$$     if (MouseCompat.getEventButton() != -1) {
    //$$         val keyCode = MouseCompat.getEventButton() - 100
    //$$         lastClickedMouseButton = keyCode
    //$$         return EventKey(keyCode, MouseCompat.getEventButtonState())
    //$$     }
    //$$     if (lastClickedMouseButton != -1 && MouseCompat.getEventButton() == -1) {
    //$$         Mouse.poll()
    //$$         val originalButton = lastClickedMouseButton + 100
    //$$         if (Mouse.isButtonDown(originalButton)) {
    //$$             return EventKey(lastClickedMouseButton, true)
    //$$         } else {
    //$$             lastClickedMouseButton = -1
    //$$         }
    //$$     }
    //$$     return null
    //$$ }
    //$$
    //$$ private val pressedKeys = mutableSetOf<Int>()
    //$$
    //$$ private fun getSyntheticKeyboardKeyCode(key: Int, char: Char): Int = if (key == 0) char.code + 256 else key
    //#endif

    //#if MC < 1.16
    //$$ @HandleEvent(priority = HandleEvent.LOWEST)
    //$$ fun onTick() {
    //$$     val currentScreen = Minecraft.getMinecraft().currentScreen
    //$$     val isConfigScreen = currentScreen is GuiScreenElementWrapper
    //$$     if (isConfigScreen || currentScreen is GuiChat) return
    //$$
    //$$     val keys: List<EventKey> = buildList {
    //$$         getKeyboardEventKey()?.let { add(it) }
    //$$         getMouseEventKey()?.let { add(it) }
    //$$     }
    //$$
    //$$     for (key in keys) {
    //$$         if (key.pressed && !pressedKeys.contains(key.keyCode)) {
    //$$             postKeyDownEvent(key.keyCode)
    //$$             pressedKeys.add(key.keyCode)
    //$$         }
    //$$     }
    //$$
    //$$     for (keyCode in pressedKeys.toList()) {
    //$$         val isDown = if (keyCode < 0) {
    //$$             Mouse.isButtonDown(keyCode + 100)
    //$$         } else {
    //$$             if (keyCode < Keyboard.KEYBOARD_SIZE) {
    //$$                 Keyboard.isKeyDown(keyCode)
    //$$             } else {
    //$$                 false
    //$$             }
    //$$         }
    //$$
    //$$         if (isDown) {
    //$$             postKeyPressEvent(keyCode)
    //$$         } else {
    //$$             postKeyUpEvent(keyCode)
    //$$             pressedKeys.remove(keyCode)
    //$$         }
    //$$     }
    //$$ }
    //#endif
    // on 1.21 we use MixinKeyboard, it provides all of this

    /*
    The delay below is here to make sure the Text input features in graph editor
    and in renderable calls have time to react first, and lock this key press event properly
     */

    // On 1.21 we post these events inside mixins
    //#if MC < 1.21
    //$$ private fun postKeyPressEvent(keyCode: Int) {
    //$$     DelayedRun.runDelayed(50.milliseconds) {
    //$$         if (TextInput.isActive()) return@runDelayed
    //$$         KeyPressEvent(keyCode).post()
    //$$     }
    //$$ }
    //$$
    //$$ private fun postKeyDownEvent(keyCode: Int) {
    //$$     DelayedRun.runDelayed(50.milliseconds) {
    //$$         if (TextInput.isActive()) return@runDelayed
    //$$         KeyDownEvent(keyCode).post()
    //$$     }
    //$$ }
    //$$
    //$$ private fun postKeyUpEvent(keyCode: Int) {
    //$$     DelayedRun.runDelayed(50.milliseconds) {
    //$$         if (TextInput.isActive()) return@runDelayed
    //$$         KeyUpEvent(keyCode).post()
    //$$     }
    //$$ }
    //#endif

    fun KeyMapping.isActive(): Boolean {
        //#if MC < 1.16
        //$$ if (!Keyboard.isCreated()) return false
        //#endif
        try {
            if (key.value.isKeyHeld()) return true
        } catch (e: IndexOutOfBoundsException) {
            ErrorManager.logErrorWithData(
                e,
                "Error while checking if a key is pressed.",
                "keyCode" to key.value,
            )
            return false
        }
        return this.isDown || this.consumeClick()
    }

    fun Int.isKeyHeld(): Boolean = when {
        //#if MC < 1.16
        //$$ this == 0 -> false
        //$$ this < 0 -> MouseCompat.isButtonDown(this + 100)
        //$$ this >= Keyboard.KEYBOARD_SIZE -> {
        //$$     val pressedKey = if (Keyboard.getEventKey() == 0) Keyboard.getEventCharacter().code + 256 else Keyboard.getEventKey()
        //$$     Keyboard.getEventKeyState() && this == pressedKey
        //$$ }
        //$$
        //$$ else -> Keyboard.isKeyDown(this)
        //#else
        this < -1 -> ErrorManager.skyHanniError("Error while checking if a key is pressed. Keycode is invalid: $this")
        this == -1 -> false
        this in 0..5 -> MouseCompat.isButtonDown(this)
        //#if MC < 1.21.9
        else -> InputConstants.isKeyDown(Minecraft.getInstance().window.window, this)
        //#else
        //$$ else -> InputUtil.isKeyPressed(MinecraftClient.getInstance().window, this)
        //#endif
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

    fun getKeyName(keyCode: Int): String = IMinecraft.INSTANCE.getKeyName(keyCode).text

    object WasdInputMatrix : Iterable<KeyMapping> {
        operator fun contains(keyBinding: KeyMapping) = when (keyBinding) {
            w, a, s, d, up, down -> true
            else -> false
        }

        val w get() = Minecraft.getInstance().options.keyUp!!
        val a get() = Minecraft.getInstance().options.keyLeft!!
        val s get() = Minecraft.getInstance().options.keyDown!!
        val d get() = Minecraft.getInstance().options.keyRight!!

        val up get() = Minecraft.getInstance().options.keyJump!!
        val down get() = Minecraft.getInstance().options.keyShift!!

        override fun iterator(): Iterator<KeyMapping> =
            object : Iterator<KeyMapping> {

                var current = w
                var finished = false

                override fun hasNext(): Boolean =
                    !finished

                override fun next(): KeyMapping {
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
