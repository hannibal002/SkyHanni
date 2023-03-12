package at.hannibal2.skyhanni.events

import net.minecraft.item.ItemStack

class GardenToolChangeEvent(val crop: String?, val heldItem: ItemStack?) : LorenzEvent()