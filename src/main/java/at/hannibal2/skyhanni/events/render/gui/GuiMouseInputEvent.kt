package at.hannibal2.hanni.events.render.gui

import at.hannibal2.hanni.api.event.CancellableHanniEvent
import net.minecraft.client.gui.GuiScreen

class GuiMouseInputEvent(val gui: GuiScreen) : CancellableHanniEvent()
