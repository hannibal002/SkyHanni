package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.compat.InventoryCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer

/**
 * Event that is fired when a key is pressed while a SkyHanniGuiContainer is open.
 * This event is cancellable, and if canceled, the key press will not be processed by the GUI.
 * Users of this function should use [at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld]
 * Or [at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked]
 * if they want to see which key was pressed.
 */
@PrimaryFunction("onGuiKeyPress")
sealed class GuiKeyPressEvent(
    val guiContainer: SkyHanniGuiContainer,
) : CancellableSkyHanniEvent() {
    val stackUnderCursor by lazy {
        InventoryCompat.stackUnderCursor()
    }

    @PrimaryFunction("onGuiKeyboardKeyPress")
    class GuiKeyboardKeyPressEvent(
        guiContainer: SkyHanniGuiContainer,
    ) : GuiKeyPressEvent(guiContainer)

    @PrimaryFunction("onGuiMouseKeyPress")
    class GuiMouseKeyPressEvent(
        guiContainer: SkyHanniGuiContainer,
    ) : GuiKeyPressEvent(guiContainer)
}
