package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.jsonobjects.WarpsJson
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object WarpTabComplete {
    private val config get() = SkyHanniMod.feature.commands.tabComplete
    private var warps = listOf<String>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<WarpsJson>("Warps")
        warps = data.warpCommands
    }

    fun handleTabComplete(command: String): List<String>? {
        if (!isEnabled()) return null
        if (command != "warp") return null

        return warps
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.warps
}
