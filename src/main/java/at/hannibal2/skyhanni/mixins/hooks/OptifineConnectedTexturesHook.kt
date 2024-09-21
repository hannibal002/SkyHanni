package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor
import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor.CommissionBlock.Companion.onColor
import at.hannibal2.skyhanni.features.mining.MiningCommissionsBlocksColor.replaceBlocksMapCache
import at.hannibal2.skyhanni.features.mining.OreType.Companion.isOreType
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.block.state.IBlockState

fun modifyConnectedTexturesBlockState(state: IBlockState): IBlockState {
    if (state == null) return state
    var returnState: IBlockState = state

    if (!LorenzUtils.inSkyBlock) return state

    try {
        if (MiningCommissionsBlocksColor.enabled && MiningCommissionsBlocksColor.active) {
            returnState = replaceBlocksMapCache.getOrPut(state) {
                MiningCommissionsBlocksColor.CommissionBlock.entries.firstOrNull {
                    state.isOreType(it.oreType)
                }?.onColor(state) ?: state
            }
        }
    } catch (e: Exception) {
        ErrorManager.logErrorWithData(e, "Error in MiningCommissionsBlocksColor")
    }

    if (returnState !== state) {

        return returnState
    }
    return state
}

