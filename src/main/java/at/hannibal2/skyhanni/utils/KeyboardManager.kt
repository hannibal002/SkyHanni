package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigEditorKeyMapping
import at.hannibal2.skyhanni.config.core.elements.GuiOptionEditorInputCode
import at.hannibal2.skyhanni.events.inventory.AttemptedInventoryCloseEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.processor.MoulConfigProcessor
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
        if (InputCode.KEY_RETURN.isKeyHeld()) lockedKeys.add(InputCode.KEY_RETURN.key)
        if (InputCode.KEY_NUMPADENTER.isKeyHeld()) lockedKeys.add(InputCode.KEY_NUMPADENTER.key)
    }

    /**
     * Represents whether either the left or right Super key (also known as Windows key) is down.
     * On macOS, this is the Command key.
     */
    private fun isSuperKeyDown() =
        InputCode.KEY_LSUPER.isKeyHeld() || InputCode.KEY_RSUPER.isKeyHeld()

    /**
     * Represents whether either the left or right Alt key is down.
     * On macOS, this is the Option key.
     */
    fun isMenuKeyDown() =
        InputCode.KEY_LALT.isKeyHeld() || InputCode.KEY_RALT.isKeyHeld()

    /**
     * Represents whether either the left or right Control (Ctrl) key is down,
     * regardless of platform.
     */
    fun isControlKeyDown() =
        InputCode.KEY_LCONTROL.isKeyHeld() || InputCode.KEY_RCONTROL.isKeyHeld()

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
        InputCode.KEY_BACKSPACE.isKeyHeld() && if (SystemUtils.IS_OS_MAC) isMenuKeyDown() else isControlKeyDown()

    /**
     * Represents whether the user is trying to use the operating system's "delete line" shortcut.
     * On macOS, this is Cmd+Shift+Backspace, while on other platforms it is Ctrl+Shift+Backspace.
     */
    fun isDeleteLineDown() =
        InputCode.KEY_BACKSPACE.isKeyHeld() && isModifierKeyDown() && isShiftKeyDown()

    /**
     * Represents whether either the left or right Shift key is down.
     */
    fun isShiftKeyDown() =
        InputCode.KEY_LSHIFT.isKeyHeld() || InputCode.KEY_RSHIFT.isKeyHeld()

    /**
     * Represents whether the user is trying to use the operating system's "copy" shortcut.
     * On macOS, this is Cmd+C, while on other platforms it is Ctrl+C.
     */
    fun isCopyingKeysDown() =
        isModifierKeyDown() && InputCode.KEY_C.isKeyHeld()

    /**
     * Represents whether the user is trying to use the operating system's "paste" shortcut.
     * On macOS, this is Cmd+V, while on other platforms it is Ctrl+V.
     */
    fun isPastingKeysDown() =
        isModifierKeyDown() && InputCode.KEY_V.isKeyHeld()

    private fun Int.matchesClosureKey() =
        Minecraft.getInstance().options.keyInventory.matches(KeyEvent(this, this, 0))

    @JvmStatic
    fun checkIsInventoryClosure(keycode: Int): Boolean {
        // Holding shift bypasses closure checks
        if (isShiftKeyDown()) return false

        val isClose = keycode.matchesClosureKey() || keycode == InputCode.KEY_ESCAPE.value
        if (!isClose) return false

        return AttemptedInventoryCloseEvent().post().isCancelled
    }

    fun checkIsInventoryClosure(keycode: InputCode): Boolean = checkIsInventoryClosure(keycode.value)

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
            if (key.isKeyHeld()) return true
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

    fun KeyMapping.isKeyHeld(): Boolean = key.isKeyHeld()

    fun InputConstants.Key.isKeyHeld(): Boolean {
        val value = this.value
        return when {
            value < InputCode.UNKNOWN.value -> ErrorManager.skyHanniError(
                "Error while checking if a key is pressed. Key code is invalid: $this",
            )
            value == InputCode.UNKNOWN.value -> false
            type == MOUSE -> MouseCompat.isButtonDown(value)
            else -> InputConstants.isKeyDown(
                //? if < 26.3
                //Minecraft.getInstance().window,
                value,
            )
        }
    }

    private val lockedKeys = mutableSetOf<InputConstants.Key>()

    /**
     * Can only be used once per click, since the function locks itself until the key is no longer
     * held. Do not use in [KeyPressEvent], since it won't be unlocked again – use [KeyDownEvent]
     * instead.
     */
    fun InputConstants.Key.isKeyClicked(): Boolean = if (isKeyHeld()) {
        lockedKeys.add(this)
    } else {
        lockedKeys.remove(this)
        false
    }

    fun KeyMapping.isKeyClicked(): Boolean = key.isKeyClicked()

    fun getKeyName(key: InputConstants.Key): String {
        if (key == InputCode.UNKNOWN.key) return "NONE"
        val displayName = key.displayName
        val collapsedDisplayName = displayName.tryCollapseToString() ?: displayName.string
        // In case of fallback, we will attempt to collapse the string to a more readable format.
        return when {
            collapsedDisplayName.startsWith("key.mouse.") -> "Button ${collapsedDisplayName.removePrefix("key.mouse.").capitalize()}"
            collapsedDisplayName.startsWith("key.keyboard.") -> collapsedDisplayName.removePrefix("key.keyboard.").capitalize()
            else -> collapsedDisplayName
        }
    }

    fun injectConfigProcessor(processor: MoulConfigProcessor<*>) {
        processor.registerConfigEditor(ConfigEditorKeyMapping::class.java) { option, annotation ->
            GuiOptionEditorInputCode(option, annotation.defaultKey)
        }
    }

    object WasdInputMatrix {
        val w get() = Minecraft.getInstance().options.keyUp
        val a get() = Minecraft.getInstance().options.keyLeft
        val s get() = Minecraft.getInstance().options.keyDown
        val d get() = Minecraft.getInstance().options.keyRight

        val up get() = Minecraft.getInstance().options.keyJump
        val down get() = Minecraft.getInstance().options.keyShift

        private val all get() = listOf(w, a, s, d, up, down)

        operator fun contains(keyBinding: KeyMapping) = keyBinding in all
    }
}
