package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.item.ItemStack

class ItemClickEvent(val itemInHand: ItemStack?) : LorenzEvent()