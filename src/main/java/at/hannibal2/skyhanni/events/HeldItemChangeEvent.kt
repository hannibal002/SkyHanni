package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import net.minecraft.world.item.ItemStack

@PrimaryFunction("onHeldItemChange")
data class HeldItemChangeEvent(val stack: ItemStack, val slot: Int) : SkyHanniEvent()
