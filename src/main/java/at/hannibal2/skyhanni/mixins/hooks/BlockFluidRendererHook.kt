package at.hannibal2.skyhanni.mixins.hooks

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland

object BlockFluidRendererHook {
    private val config get() = SkyHanniMod.feature.fishing.lavaReplacement

    @JvmStatic
    fun replaceLava(): Boolean {
        if (!LorenzUtils.inSkyBlock || !config.enabled) return false
        if (config.onlyInCrimsonIsle && !IslandType.CRIMSON_ISLE.isInIsland()) return false
        return true
    }
}
