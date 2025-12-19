package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.api.minecraftevents.RenderLayer
import net.minecraft.client.gui.GuiGraphics

class GameOverlayRenderPostEvent(context: GuiGraphics, val type: RenderLayer) : RenderingSkyHanniEvent(context)
