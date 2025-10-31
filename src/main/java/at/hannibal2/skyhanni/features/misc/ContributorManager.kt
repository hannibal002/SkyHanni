package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.jsonobjects.repo.ContributorJsonEntry
import at.hannibal2.hanni.data.jsonobjects.repo.ContributorsJson
import at.hannibal2.hanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.hanni.events.RepositoryReloadEvent
import at.hannibal2.hanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.chat.TextHelper.asComponent
import at.hannibal2.hanni.utils.compat.appendComponent
import net.minecraft.entity.player.EntityPlayer

@HanniModule
object ContributorManager {
    private val config get() = HanniMod.feature.dev

    // Key is the lowercase contributor name
    private var contributors: Map<String, ContributorJsonEntry> = emptyMap()

    // Just the names of the contributors including their proper case
    var contributorNames = emptyList<String>()
        private set

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val map = event.getConstant<ContributorsJson>("Contributors").contributors
        contributors = map.mapKeys { it.key.lowercase() }
        contributorNames = map.map { it.key }
    }

    @HandleEvent
    fun onRenderNametag(event: EntityDisplayNameEvent<EntityPlayer>) {
        if (!config.contributorNametags) return
        if (event.entity.isRealPlayer()) getSuffix(event.entity.name)?.let {
            event.chatComponent.appendComponent(it.asComponent())
        }
    }

    fun getSuffix(username: String): String? = getContributor(username)?.suffix

    fun shouldSpin(username: String): Boolean = getContributor(username)?.spinny ?: false
    fun shouldBeUpsideDown(username: String): Boolean = getContributor(username)?.upsideDown ?: false

    private fun getContributor(username: String) =
        contributors[username.lowercase()]?.let { it.takeIf { it.isAllowed() } }

    private fun ContributorJsonEntry.isAllowed(): Boolean {
        if (!config.fancyContributors) return false
        return when (externalMod) {
            // normal Hanni contributor
            null -> true

            // TODO add other mod's devs, e.g skytils

            "SBA" -> config.fancySbaContributors

            else -> false
        }
    }
}
