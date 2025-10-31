package at.hannibal2.hanni.events

import at.hannibal2.hanni.data.ClickType
import at.hannibal2.hanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.hanni.utils.LorenzVec
import net.minecraft.item.ItemStack

class BlockClickEvent(clickType: ClickType, val position: LorenzVec, itemInHand: ItemStack?) :
    WorldClickEvent(itemInHand, clickType) {

    val getBlockState by lazy { position.getBlockStateAt() }
}
