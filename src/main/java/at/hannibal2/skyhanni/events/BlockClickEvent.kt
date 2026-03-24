package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SafeItemStack

class BlockClickEvent(clickType: ClickType, val position: LorenzVec, itemInHand: SafeItemStack?) :
    WorldClickEvent(itemInHand, clickType) {

    val getBlockState by lazy { position.getBlockStateAt() }
}
