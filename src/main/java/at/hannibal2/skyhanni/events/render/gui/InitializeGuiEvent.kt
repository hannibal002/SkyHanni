package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import net.minecraft.client.gui.layouts.LayoutElement
import net.minecraft.client.gui.screens.Screen

class InitializeGuiEvent(val gui: Screen, val buttonList: MutableList<LayoutElement>) : SkyHanniEvent()
