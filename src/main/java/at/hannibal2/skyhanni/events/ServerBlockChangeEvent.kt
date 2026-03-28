package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.api.event.SkyHanniEvent
import at.hannibal2.skyhanni.skyhannimodule.PrimaryFunction
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockStateAt
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.VectorUtils.toVec3
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState

@PrimaryFunction("onServerBlockChange")
class ServerBlockChangeEvent(blockPos: BlockPos, blockState: BlockState) : SkyHanniEvent() {

    val location = blockPos.toVec3()
    val old by lazy { oldState.block.toString().getName() }
    val oldState by lazy { location.getBlockStateAt() }
    val new by lazy { blockState.block.toString().getName() }
    val newState = blockState

    companion object {

        private val pattern = "Block\\{minecraft:(?<name>.*)}".toPattern()

        private fun String.getName() = pattern.matchGroup(this, "name") ?: this
    }
}


