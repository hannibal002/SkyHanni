package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.RenderingSkyHanniEvent
import at.hannibal2.skyhanni.utils.compat.DrawContext
import net.minecraft.item.ItemStack

class RenderGuiItemOverlayEvent(context: DrawContext, val stack: ItemStack?, val x: Int, val y: Int) : RenderingSkyHanniEvent(context)
