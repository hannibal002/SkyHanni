package at.hannibal2.skyhanni.events.garden.visitor

import at.hannibal2.skyhanni.events.LorenzEvent
import at.hannibal2.skyhanni.features.garden.visitor.VisitorAPI
import net.minecraft.item.ItemStack

class VisitorToolTipEvent(val visitor: VisitorAPI.Visitor, val itemStack: ItemStack, val toolTip: MutableList<String>) : LorenzEvent()