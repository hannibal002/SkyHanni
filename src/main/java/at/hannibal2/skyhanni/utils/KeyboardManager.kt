package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.BlockingMoulConfigProcessor
import at.hannibal2.skyhanni.config.core.elements.GuiOptionEditorKeyMapping
import at.hannibal2.skyhanni.events.inventory.AttemptedInventoryCloseEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.KeyPressEvent
import at.hannibal2.skyhanni.events.render.gui.GuiScreenOpenEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.compat.MouseCompat
import com.mojang.blaze3d.platform.InputConstants
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorKeybind
import io.github.notenoughupdates.moulconfig.common.IMinecraft
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.input.InputQuirks
import net.minecraft.client.input.KeyEvent
import net.minecraft.resources.Identifier
import org.apache.commons.lang3.SystemUtils
import org.lwjgl.glfw.GLFW

@SkyHanniModule
object KeyboardManager {

    // When a screen closes (e.g. chat closed via Enter), lock Enter so it does not
    // immediately fire as a key click in features that use isKeyClicked().
    @HandleEvent
    fun onGuiOpen(event: GuiScreenOpenEvent) {
        if (event.gui != null) return
        if (GLFW.GLFW_KEY_ENTER.isKeyHeld()) lockedKeys.add(GLFW.GLFW_KEY_ENTER)
        if (GLFW.GLFW_KEY_KP_ENTER.isKeyHeld()) lockedKeys.add(GLFW.GLFW_KEY_KP_ENTER)
    }

    const val LEFT_MOUSE = GLFW.GLFW_MOUSE_BUTTON_LEFT
    const val RIGHT_MOUSE = GLFW.GLFW_MOUSE_BUTTON_RIGHT
    const val MIDDLE_MOUSE = GLFW.GLFW_MOUSE_BUTTON_MIDDLE

    /**
     * Represents whether either the left or right Super key (also known as Windows key) is down.
     * On macOS, this is the Command key.
     */
    private fun isSuperKeyDown() =
        GLFW.GLFW_KEY_LEFT_SUPER.isKeyHeld() || GLFW.GLFW_KEY_RIGHT_SUPER.isKeyHeld()

    /**
     * Represents whether either the left or right Alt key is down.
     * On macOS, this is the Option key.
     */
    fun isMenuKeyDown() =
        GLFW.GLFW_KEY_LEFT_ALT.isKeyHeld() || GLFW.GLFW_KEY_RIGHT_ALT.isKeyHeld()

    /**
     * Represents whether either the left or right Control (Ctrl) key is down,
     * regardless of platform.
     */
    fun isControlKeyDown() =
        GLFW.GLFW_KEY_LEFT_CONTROL.isKeyHeld() || GLFW.GLFW_KEY_RIGHT_CONTROL.isKeyHeld()

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
        GLFW.GLFW_KEY_BACKSPACE.isKeyHeld() && if (SystemUtils.IS_OS_MAC) isMenuKeyDown() else isControlKeyDown()

    /**
     * Represents whether the user is trying to use the operating system's "delete line" shortcut.
     * On macOS, this is Cmd+Shift+Backspace, while on other platforms it is Ctrl+Shift+Backspace.
     */
    fun isDeleteLineDown() =
        GLFW.GLFW_KEY_BACKSPACE.isKeyHeld() && isModifierKeyDown() && isShiftKeyDown()

    /**
     * Represents whether either the left or right Shift key is down.
     */
    fun isShiftKeyDown() =
        GLFW.GLFW_KEY_LEFT_SHIFT.isKeyHeld() || GLFW.GLFW_KEY_RIGHT_SHIFT.isKeyHeld()

    /**
     * Represents whether the user is trying to use the operating system's "copy" shortcut.
     * On macOS, this is Cmd+C, while on other platforms it is Ctrl+C.
     */
    fun isCopyingKeysDown() =
        isModifierKeyDown() && GLFW.GLFW_KEY_C.isKeyHeld()

    /**
     * Represents whether the user is trying to use the operating system's "paste" shortcut.
     * On macOS, this is Cmd+V, while on other platforms it is Ctrl+V.
     */
    fun isPastingKeysDown() =
        isModifierKeyDown() && GLFW.GLFW_KEY_V.isKeyHeld()

    private fun Int.matchesClosureKey() =
        Minecraft.getInstance().options.keyInventory.matches(KeyEvent(this, this, 0))

    @JvmStatic
    fun checkIsInventoryClosure(keycode: Int): Boolean {
        // Holding shift bypasses closure checks
        if (isShiftKeyDown()) return false

        val isClose = keycode.matchesClosureKey() || keycode == GLFW.GLFW_KEY_ESCAPE
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
        this in 0..5 -> MouseCompat.isButtonDown(this)
        else -> InputConstants.isKeyDown(Minecraft.getInstance().window, this)
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

    private val SKYHANNI = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("skyhanni", "keys")
    )

    fun injectConfigProcessor(processor: BlockingMoulConfigProcessor) {
        processor.registerConfigEditor(ConfigEditorKeybind::class.java) { option, annotation ->
            val displayName = getDisplayNameForKeyMapping(option)
            val mapping = getOrCreateKeyMapping(
                option.get() as Int,
                annotation.defaultKey,
                // HACK: it's supposed to take a translation key
                displayName,
            )
            GuiOptionEditorKeyMapping(option, mapping)
        }
    }

    private fun getDisplayNameForKeyMapping(option: ProcessedOption): String {
        val parts = option.path.split(".")
        // wardrobe.keybinds.open -> wardrobe.open
        // dev.chat.peekKey -> chat.peekKey
        val name = if (parts.size >= 3 && parts[parts.lastIndex - 1].startsWith("keybind", true)) {
            listOf(parts[parts.lastIndex - 2], parts.last())
        } else {
            parts.takeLast(2)
        }

        // wardrobe.open -> Wardrobe Open
        return name
            .joinToString(" ")
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .replace(Regex("([A-Za-z])([0-9])"), "$1 $2")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private val keyMappingMap = mutableMapOf<String, KeyMapping>()

    private fun getOrCreateKeyMapping(key: Int, defaultKey: Int, id: String): KeyMapping =
        keyMappingMap.computeIfAbsent(id) {
            createKeyMapping(key, defaultKey, id)
        }

    fun createKeyMapping(key: Int, defaultKey: Int, id: String): KeyMapping {
        val type = if (key in 0..5) InputConstants.Type.MOUSE else InputConstants.Type.KEYSYM
        val keyMapping = KeyMapping(
            id,
            type,
            defaultKey,
            SKYHANNI,
        )
        val key = type.getOrCreate(key)
        keyMapping.setKey(key)
        KeyMappingHelper.registerKeyMapping(keyMapping)
        return keyMapping
    }

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
