package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos

class ServerBlockChangeEvent(val pos: BlockPos, val blockState: IBlockState) : LorenzEvent() {
    val location by lazy { pos.toLorenzVec() }
    val old by lazy { location.getBlockAt().toString().getName() }
    val new by lazy { blockState.block.toString().getName() }

    companion object {
        val pattern = "Block\\{minecraft:(?<name>.*)}".toPattern()

        private fun String.getName() = pattern.matchMatcher(this) {
            group("name")
        } ?: this

    }

}


