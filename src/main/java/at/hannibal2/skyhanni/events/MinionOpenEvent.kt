package at.hannibal2.skyhanni.events

import net.minecraft.item.ItemStack

class MinionOpenEvent(val inventoryName: String, val inventoryItems: Map<Int, ItemStack>) : LorenzEvent()