package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LorenzVec
import net.minecraft.world.item.ItemStack

class BlockClickEvent(clickType: InteractClickType, val position: LorenzVec, itemInHand: ItemStack?) :
    WorldClickEvent(itemInHand, clickType) {

    val getBlockState by lazy { position.getBlockStateAt() }
}
