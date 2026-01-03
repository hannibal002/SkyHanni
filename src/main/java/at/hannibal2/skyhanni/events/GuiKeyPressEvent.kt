package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.SkyHanniGuiContainer

class GuiKeyPressEvent(val guiContainer: SkyHanniGuiContainer) : CancellableSkyHanniEvent()
