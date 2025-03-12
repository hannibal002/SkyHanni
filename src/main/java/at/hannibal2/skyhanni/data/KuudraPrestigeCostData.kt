package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.KuudraPrestigeCostsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName

@SkyHanniModule
object KuudraPrestigeCostData {
    private var prestigeCosts = emptyMap<String, Map<NeuInternalName, Int>>()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val costs = event.getConstant<KuudraPrestigeCostsJson>("KuudraPrestigeCosts")
        prestigeCosts = costs.kuudraPrestigeCost
    }

    fun getPrestigeCostByNameOrNull(name: String): Map<NeuInternalName, Int>? = prestigeCosts[name]
}

