package at.hannibal2.skyhanni.events

import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos

class ServerBlockChangeEvent(private val blockPos: BlockPos, private val blockState: IBlockState) : LorenzEvent() {

    val location by lazy { blockPos.toLorenzVec() }
    val old by lazy { location.getBlockAt().toString().getName() }
    val new by lazy { blockState.block.toString().getName() }

    companion object {

        val pattern = "Block\\{minecraft:(?<name>.*)}".toPattern()

        private fun String.getName() = pattern.matchMatcher(this) {
            group("name")
        } ?: this
    }
}


