package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.Thread
import net.minecraft.client.gui.screens.Screen

@Thread(RENDER)
class GuiScreenOpenEvent(val gui: Screen?) : SkyHanniEvent()
