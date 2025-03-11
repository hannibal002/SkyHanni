package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.KuudraPrestigeCostsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName

@SkyHanniModule
object KuudraPrestigeCostData {
    var prestigeCosts = emptyMap<String, PrestigeCost>()
        private set

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val costs = event.getConstant<KuudraPrestigeCostsJson>("KuudraPrestigeCosts")
        prestigeCosts = costs.kuudraPrestigeCost.mapValues { (_, value) ->
            PrestigeCost(value.crimsonEssence, value.kuudraTeeth, value.coins)
        }
    }

    fun getPrestigeCostByNameOrNull(name: String): PrestigeCost? {
        return prestigeCosts[name]
    }
}

data class PrestigeCost(
    val crimsonEssence: Int,
    val kuudraTeeth: Int,
    val coins: Int,
) {
    val asCostMap: Map<NeuInternalName, Int>
        get() = mapOf(
            "ESSENCE_CRIMSON".toInternalName() to crimsonEssence,
            "KUUDRA_TEETH".toInternalName() to kuudraTeeth,
            "SKYBLOCK_COIN".toInternalName() to coins,
        )
}
