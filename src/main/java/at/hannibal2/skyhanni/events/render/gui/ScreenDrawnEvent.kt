package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext
import net.minecraft.client.gui.GuiScreen

class ScreenDrawnEvent(val context: DrawContext, val gui: GuiScreen?) : SkyHanniEvent()
