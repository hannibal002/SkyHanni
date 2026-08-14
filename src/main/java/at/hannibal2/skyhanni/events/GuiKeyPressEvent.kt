package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer

/**
 * Fired when a key or mouse button is pressed while a container GUI is open.
 *
 * Note: despite the name, this event also covers mouse button inputs. The [isMouseInput]
 * flag distinguishes between the two.
 *
 * @property guiContainer the container GUI that received the input.
 * @property isMouseInput true if triggered by a mouse button, false for keyboard keys.
 */
@PrimaryFunction("onGuiKeyPress")
class GuiKeyPressEvent(val guiContainer: SkyHanniGuiContainer, val isMouseInput: Boolean) : CancellableSkyHanniEvent()
// TODO A cleaner solution passing key IDs should replace isMouseInput in the future.
