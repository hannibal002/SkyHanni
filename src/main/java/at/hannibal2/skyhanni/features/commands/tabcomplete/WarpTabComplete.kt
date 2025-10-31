package at.hannibal2.hanni.features.commands.tabcomplete

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.jsonobjects.repo.WarpsJson
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.events.chat.TabCompletionEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.SkyBlockUtils

@HanniModule
object WarpTabComplete {

    private val config get() = HanniMod.feature.misc.commands.tabComplete
    private var warps = emptyList<String>()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<WarpsJson>("Warps")
        warps = data.warpCommands
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTabComplete(event: TabCompletionEvent) {
        if (event.isCommand("warp")) {
            event.addSuggestions(warps)
        }
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.warps
}
