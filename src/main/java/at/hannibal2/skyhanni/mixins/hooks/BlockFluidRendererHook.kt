package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material

object BlockFluidRendererHook {
    private val config get() = SkyHanniMod.feature.fishing.lavaReplacementConfig

    @JvmStatic
    fun replaceLava(block: BlockLiquid): Material {
        if (!LorenzUtils.inSkyBlock || !config.enabled) return block.material
        if (config.onlyInCrimsonIsle && !IslandType.CRIMSON_ISLE.isInIsland()) return block.material
        return Material.water
    }
}
