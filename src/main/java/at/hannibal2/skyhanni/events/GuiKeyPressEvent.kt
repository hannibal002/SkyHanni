package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.events.render.gui.GuiMouseInputEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer

/**
 * Fired when a key is pressed or a mouse button is clicked while a container screen is open.
 * Despite the name, this covers mouse input as well.
 *
 * The event carries no information about which input triggered it, so listeners have to check the
 * key or mouse button themselves, for example through `KeyboardManager.isKeyClicked()`.
 * Cancelling it stops the screen from handling the input.
 *
 * For mouse input specifically, prefer [GuiMouseInputEvent], which is fired alongside this one.
 *
 * @param guiContainer The container screen that received the input.
 */
@PrimaryFunction("onGuiKeyPress")
class GuiKeyPressEvent(val guiContainer: SkyHanniGuiContainer) : CancellableSkyHanniEvent()
