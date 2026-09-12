package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

@PrimaryFunction("onBlockChange")
class ServerBlockChangeEvent(blockPos: BlockPos, blockState: BlockState) : SkyHanniEvent() {
    val location = blockPos.toLorenzVec()

    val oldState by lazy { location.getBlockStateAt() }
    val old: Block by lazy { oldState.block }

    val newState = blockState
    val new: Block by lazy { oldState.block }
}
