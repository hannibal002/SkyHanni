package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer

@PrimaryFunction("onGuiKeyPress")
class GuiKeyPressEvent(val guiContainer: SkyHanniGuiContainer) : CancellableSkyHanniEvent()
