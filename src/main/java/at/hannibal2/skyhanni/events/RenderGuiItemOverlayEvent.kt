package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

class RenderGuiItemOverlayEvent(context: GuiGraphics, val stack: ItemStack?, val x: Int, val y: Int) : RenderingSkyHanniEvent(context)
