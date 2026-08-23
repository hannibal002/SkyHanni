package at.hannibal2.skyhanni.events.render.gui

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Fired after the background texture of a container screen has been rendered,
 * but before slots, items, and labels are drawn.
 *
 * Only fires when a local world and player exist.
 */
@PrimaryFunction("onBackgroundDraw")
class DrawBackgroundEvent(context: GuiGraphicsExtractor) : RenderingSkyHanniEvent(context)
