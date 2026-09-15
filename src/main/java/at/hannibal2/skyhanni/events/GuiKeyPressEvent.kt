package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.InventoryCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer

/**
 * Fired when a key is pressed or a mouse button is clicked while a container screen is open.
 * Despite the name, this covers mouse input as well.
 *
 * Listen to this class to receive both, or to [GuiKeyboardKeyPressEvent] or
 * [GuiMouseButtonPressEvent] to receive only one of the two.
 *
 * The event carries no information about which key or button triggered it, so listeners have to
 * check that themselves, for example through `KeyboardManager.isKeyHeld()` or
 * `KeyboardManager.isKeyClicked()`.
 * Cancelling it stops the screen from handling the input.
 *
 * @param guiContainer The container screen that received the input.
 */
@PrimaryFunction("onGuiKeyPress")
sealed class GuiKeyPressEvent(
    val guiContainer: SkyHanniGuiContainer,
) : CancellableSkyHanniEvent() {
    val stackUnderCursor: SafeItemStack? by lazy {
        InventoryCompat.stackUnderCursor()
    }

    /**
     * Fired when a keyboard key is pressed while a container screen is open.
     *
     * Cancelling this event stops the screen from handling the keyboard input.
     *
     * @param guiContainer The container screen that received the keyboard input.
     */
    @PrimaryFunction("onGuiKeyboardKeyPress")
    class GuiKeyboardKeyPressEvent(
        guiContainer: SkyHanniGuiContainer,
    ) : GuiKeyPressEvent(guiContainer)

    /**
     * Fired when a mouse button is pressed or clicked while a container screen is open.
     *
     * Cancelling this event stops the screen from handling the mouse input.
     *
     * @param guiContainer The container screen that received the mouse input.
     */
    @PrimaryFunction("onGuiMouseButtonPress")
    class GuiMouseButtonPressEvent(
        guiContainer: SkyHanniGuiContainer,
    ) : GuiKeyPressEvent(guiContainer)
}
