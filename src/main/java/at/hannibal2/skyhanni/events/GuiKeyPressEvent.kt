package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.compat.InventoryCompat
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent

/**
 * Event that is fired when a key is pressed while a SkyHanniGuiContainer is open.
 * This event is cancellable, and if canceled, the key press will not be processed by the GUI.
 * Users of this function should use [at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld]
 * Or [at.hannibal2.skyhanni.utils.KeyboardManager.isKeyClicked]
 * if they want to see which key was pressed.
 */
@PrimaryFunction("onGuiKeyPress")
class GuiKeyPressEvent(
    val guiContainer: SkyHanniGuiContainer,
    private val keyEvent: KeyEvent?,
    private val mouseEvent: MouseButtonEvent?,
) : CancellableSkyHanniEvent() {
    constructor(guiContainer: SkyHanniGuiContainer, keyEvent: KeyEvent) :
        this(guiContainer, keyEvent = keyEvent, mouseEvent = null)
    constructor(guiContainer: SkyHanniGuiContainer, mouseEvent: MouseButtonEvent) :
        this(guiContainer, keyEvent = null, mouseEvent = mouseEvent)

    fun stackUnderCursor(): SafeItemStack? {
        if (mouseEvent != null) {
            return InventoryCompat.stackUnderCursor(mouseEvent)
        } else if (keyEvent != null) {
            return InventoryCompat.stackUnderCursor(keyEvent)
        }
        return null
    }
}
