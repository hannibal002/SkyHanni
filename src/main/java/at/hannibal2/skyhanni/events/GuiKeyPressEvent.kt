package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer

/**
 * @param guiContainer passes the SkyHanniGuiContainer of the Gui on key press.
 * @param isMouseBasedEvent boolean indicating whether the event was fired by a Mouse Button.
 */
@PrimaryFunction("onGuiKeyPress")
class GuiKeyPressEvent(val guiContainer: SkyHanniGuiContainer, val isMouseBasedEvent: Boolean) : CancellableSkyHanniEvent()
