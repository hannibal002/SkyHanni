package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.client.gui.widget.Widget
import net.minecraft.client.gui.screen.Screen

class InitializeGuiEvent(val gui: Screen, val buttonList: MutableList<Widget>) : SkyHanniEvent()
