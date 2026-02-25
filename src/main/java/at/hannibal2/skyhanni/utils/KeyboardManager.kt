package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.events.inventory.AttemptedInventoryCloseEvent
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import org.apache.commons.lang3.SystemUtils
import org.lwjgl.glfw.GLFW
import net.minecraft.client.input.KeyEvent

object KeyboardManager {

    const val LEFT_MOUSE = GLFW.GLFW_MOUSE_BUTTON_LEFT
    const val RIGHT_MOUSE = GLFW.GLFW_MOUSE_BUTTON_RIGHT
    const val MIDDLE_MOUSE = GLFW.GLFW_MOUSE_BUTTON_MIDDLE

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

    private fun Int.matchesClosureKey() = Minecraft.getInstance().options.keyInventory.matches(KeyEvent(this, this, 0))

    @JvmStatic
    fun checkIsInventoryClosure(keycode: Int): Boolean {
        // Holding shift bypasses closure checks
        if (isShiftKeyDown()) return false

        val isClose = keycode.matchesClosureKey() || keycode == GLFW.GLFW_KEY_ESCAPE
        if (!isClose) return false

        return AttemptedInventoryCloseEvent().post()
    }

    /**
     * TODO make use of this function unnecessary: Try to avoid using `isModifierKeyDown` as the only option,
     * allow the user to set a different option instead and just set the default key to isModifierKeyDown
     */
    fun getModifierKeyName(): String = if (SystemUtils.IS_OS_MAC) "Command" else "Control"

    /*
    The delay below is here to make sure the Text input features in graph editor
    and in renderable calls have time to react first, and lock this key press event properly
     */

    fun KeyMapping.isActive(): Boolean {
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
        this < -1 -> ErrorManager.skyHanniError("Error while checking if a key is pressed. Keycode is invalid: $this")
        this == -1 -> false
        this in 0..5 -> MouseCompat.isButtonDown(this)
        else -> InputConstants.isKeyDown(Minecraft.getInstance().window, this)
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
