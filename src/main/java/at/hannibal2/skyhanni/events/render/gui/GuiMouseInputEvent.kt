package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.CancellableSkyHanniEvent
import at.hannibal2.skyhanni.api.event.Thread
import net.minecraft.client.gui.screens.Screen

@Thread(RENDER)
class GuiMouseInputEvent(val gui: Screen) : CancellableSkyHanniEvent()
