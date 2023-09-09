package at.hannibal2.skyhanni.features.misc.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.jsonobjects.SackListJson
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object GetFromSacksTabComplete {
    private val config get() = SkyHanniMod.feature.misc.tabCompleteCommands
    private var sackListJson: SackListJson? = null
    private val commands = arrayOf("gfs", "getfromsacks")

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        sackListJson = event.getConstant<SackListJson>("Sacks")
    }

    fun handleTabComplete(command: String): List<String>? {
        if (!isEnabled()) return null
        if (command !in commands) return null

        return sackListJson?.sackList
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.gfsSack
}