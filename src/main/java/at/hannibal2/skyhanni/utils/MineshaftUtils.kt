package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.CorpseJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.features.mining.glacitemineshaft.MineshaftDetection
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule

@SkyHanniModule
object MineshaftUtils {
    var corpseLocations = mapOf<MineshaftDetection.MineshaftType, List<LorenzVec>>()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        corpseLocations = event.getConstant<CorpseJson>("FrozenCorpses").locations
    }
}
