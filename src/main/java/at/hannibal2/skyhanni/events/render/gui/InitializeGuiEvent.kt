package at.hannibal2.hanni.events.render.gui

import at.hannibal2.hanni.api.event.HanniEvent
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen

class InitializeGuiEvent(val gui: GuiScreen, val buttonList: MutableList<GuiButton>) : HanniEvent()
