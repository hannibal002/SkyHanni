package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.NEUInternalName
import net.minecraft.item.ItemStack

class ItemInHandChangeEvent(val internalName: NEUInternalName, val stack: ItemStack?) : LorenzEvent()