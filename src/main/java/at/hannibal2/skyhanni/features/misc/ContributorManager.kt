package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorJsonEntry
import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ContributorManager {

    private var contributors: Map<String, ContributorJsonEntry> = emptyMap()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        contributors = event.getConstant<ContributorsJson>("Contributors").contributors.mapKeys { it.key.lowercase() }
    }

    fun getTabListSuffix(username: String): String? {
        return contributors[username.lowercase()]?.suffix
    }

    fun canSpin(username: String): Boolean {
        return contributors[username.lowercase()]?.spinny ?: false
    }
}
