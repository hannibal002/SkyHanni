package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SafeItemStack

/**
 * Fired when the player clicks on a block.
 * Covers both left-click (initial break and continued breaking when the block position changes) and right-click.
 */
@PrimaryFunction("onBlockClick")
class BlockClickEvent(clickType: InteractClickType, val position: LorenzVec, itemInHand: SafeItemStack?) :
    WorldClickEvent(itemInHand, clickType) {

    val blockState by lazy { position.getBlockStateAt() }
}
