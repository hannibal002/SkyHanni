package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.block.state.IBlockState

fun modifyConnectedTexturesBlockState(state: IBlockState?): IBlockState? {
    if (!SkyBlockUtils.inSkyBlock) return state
    return MiningCommissionsBlocksColor.processState(state)
}

