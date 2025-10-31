package at.hannibal2.hanni.mixins.hooks

import at.hannibal2.hanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.hanni.utils.SkyBlockUtils
import net.minecraft.block.state.IBlockState

fun modifyConnectedTexturesBlockState(state: IBlockState?): IBlockState? {
    if (!SkyBlockUtils.inSkyBlock) return state
    return MiningCommissionsBlocksColor.processState(state)
}

