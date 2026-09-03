package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.inventory.AttemptedInventoryCloseEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.input.InputQuirks
import net.minecraft.client.input.KeyEvent
import org.apache.commons.lang3.SystemUtils

@SkyHanniModule
object KeyboardManager {
    // When a screen closes (e.g. chat closed via Enter), lock Enter so it does not
    // immediately fire as a key click in features that use isKeyClicked().
    @HandleEvent
    private fun onGuiOpen(event: GuiScreenOpenEvent) {
        if (event.gui != null) return
        if (InputConstants.KEY_RETURN.isKeyHeld()) lockedKeys.add(InputConstants.KEY_RETURN)
        if (InputConstants.KEY_NUMPADENTER.isKeyHeld()) lockedKeys.add(InputConstants.KEY_NUMPADENTER)
    }

    // InputConstants.UNKNOWN exists, but is not a compile time constant
    //~ if < 26.3 '0' -> '-1'
    const val KEY_UNKNOWN: Int = 0

    const val LEFT_MOUSE = InputConstants.MOUSE_BUTTON_LEFT
    const val RIGHT_MOUSE = InputConstants.MOUSE_BUTTON_RIGHT
    const val MIDDLE_MOUSE = InputConstants.MOUSE_BUTTON_MIDDLE

    const val KEY_ADD = InputConstants.KEY_ADD
    // This constant isn't defined in InputConstants for some reason
    const val KEY_SUBTRACT = InputConstants.KEY_ADD - 1

    /**
     * Represents whether either the left or right Super key (also known as Windows key) is down.
     * On macOS, this is the Command key.
     */
    private fun isSuperKeyDown(): Boolean {
        //~ if < 26.3 'GUI' -> 'SUPER'
        return InputConstants.KEY_LGUI.isKeyHeld() || InputConstants.KEY_RGUI.isKeyHeld()
    }

    /**
     * Represents whether either the left or right Alt key is down.
     * On macOS, this is the Option key.
     */
    fun isMenuKeyDown() =
        InputConstants.KEY_LALT.isKeyHeld() || InputConstants.KEY_RALT.isKeyHeld()

    /**
     * Represents whether either the left or right Control (Ctrl) key is down,
     * regardless of platform.
     */
    fun isControlKeyDown() =
        InputConstants.KEY_LCONTROL.isKeyHeld() || InputConstants.KEY_RCONTROL.isKeyHeld()

    /**
     * Represents whether the operating system's modifier key is down.
     * On macOS, this is Command (Cmd), while on other platforms it is Control (Ctrl).
     */
    fun isModifierKeyDown() =
        if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) isSuperKeyDown() else isControlKeyDown()

    /**
     * Represents whether the user is trying to use the operating system's "delete word" shortcut.
     * On macOS, this is Option+Backspace, while on other platforms it is Ctrl+Backspace.
     */
    fun isDeleteWordDown() =
        InputConstants.KEY_BACKSPACE.isKeyHeld() && if (SystemUtils.IS_OS_MAC) isMenuKeyDown() else isControlKeyDown()

    /**
     * Represents whether the user is trying to use the operating system's "delete line" shortcut.
     * On macOS, this is Cmd+Shift+Backspace, while on other platforms it is Ctrl+Shift+Backspace.
     */
    fun isDeleteLineDown() =
        InputConstants.KEY_BACKSPACE.isKeyHeld() && isModifierKeyDown() && isShiftKeyDown()

    /**
     * Represents whether either the left or right Shift key is down.
     */
    fun isShiftKeyDown() =
        InputConstants.KEY_LSHIFT.isKeyHeld() || InputConstants.KEY_RSHIFT.isKeyHeld()

    /**
     * Represents whether the user is trying to use the operating system's "copy" shortcut.
     * On macOS, this is Cmd+C, while on other platforms it is Ctrl+C.
     */
    fun isCopyingKeysDown() =
        isModifierKeyDown() && InputConstants.KEY_C.isKeyHeld()

    /**
     * Represents whether the user is trying to use the operating system's "paste" shortcut.
     * On macOS, this is Cmd+V, while on other platforms it is Ctrl+V.
     */
    fun isPastingKeysDown() =
        isModifierKeyDown() && InputConstants.KEY_V.isKeyHeld()

    private fun Int.matchesClosureKey() =
        Minecraft.getInstance().options.keyInventory.matches(KeyEvent(this, this, 0))

    @JvmStatic
    fun checkIsInventoryClosure(keycode: Int): Boolean {
        // Holding shift bypasses closure checks
        if (isShiftKeyDown()) return false

        val isClose = keycode.matchesClosureKey() || keycode == InputConstants.KEY_ESCAPE
        if (!isClose) return false

        return AttemptedInventoryCloseEvent().post().isCancelled
    }

    fun getModifierKeyName(short: Boolean = false): String =
        if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) {
            if (short) "Cmd" else "Command"
        } else {
            if (short) "Ctrl" else "Control"
        }

    // The delay below is here to make sure the Text input features in graph editor
    // and in renderable calls have time to react first, and lock this key press event properly.
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
        return isDown || consumeClick()
    }

    fun Int.isKeyHeld(): Boolean = when {
        this < -1 -> ErrorManager.skyHanniError(
            "Error while checking if a key is pressed. Key code is invalid: $this",
        )

        this == -1 -> false
        MouseCompat.isMouseButton(this) -> MouseCompat.isButtonDown(this)
        else -> InputConstants.isKeyDown(
            //? if < 26.3
            //Minecraft.getInstance().window,
            this,
        )
    }

    private val lockedKeys = mutableSetOf<Int>()

    /**
     * Can only be used once per click, since the function locks itself until the key is no longer
     * held. Do not use in [KeyPressEvent], since it won't be unlocked again – use [KeyDownEvent]
     * instead.
     */
    fun Int.isKeyClicked(): Boolean = if (isKeyHeld()) {
        lockedKeys.add(this)
    } else {
        lockedKeys.remove(this)
        false
    }

    fun getKeyName(keyCode: Int): String = IMinecraft.INSTANCE.getKeyName(keyCode).text

    object WasdInputMatrix : Iterable<KeyMapping> {
        operator fun contains(keyBinding: KeyMapping) = when (keyBinding) {
            w, a, s, d, up, down -> true
            else -> false
        }

        val w get() = Minecraft.getInstance().options.keyUp
        val a get() = Minecraft.getInstance().options.keyLeft
        val s get() = Minecraft.getInstance().options.keyDown
        val d get() = Minecraft.getInstance().options.keyRight

        val up get() = Minecraft.getInstance().options.keyJump
        val down get() = Minecraft.getInstance().options.keyShift

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
