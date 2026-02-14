package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorJsonEntry
import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.compat.append
import net.minecraft.world.entity.player.Player
import java.util.UUID

@SkyHanniModule
object ContributorManager {
    private val config get() = SkyHanniMod.feature.dev

    var contributors: Map<UUID, ContributorJsonEntry> = emptyMap()
        private set
    var contributorNames = emptyList<String>()
        private set

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val map = event.getConstant<ContributorsJson>("Contributors").contributors
        contributors = map.mapKeys { UUID.fromString(it.key) }
    }

    @HandleEvent
    fun onRenderNametag(event: EntityDisplayNameEvent<Player>) {
        if (!config.contributorNametags) return

        val gameProfile = event.entity.gameProfile
        getSuffix(gameProfile.id)?.let {
            event.chatComponent.append(it)
        }
    }

    fun getSuffix(uuid: UUID): String? = getContributor(uuid)?.suffix

    fun shouldSpin(uuid: UUID): Boolean = getContributor(uuid)?.spinny ?: false
    fun shouldBeUpsideDown(uuid: UUID): Boolean = getContributor(uuid)?.upsideDown ?: false

    private fun getContributor(uuid: UUID) =
        contributors[uuid]?.let { it.takeIf { it.isAllowed() } }

    private fun ContributorJsonEntry.isAllowed(): Boolean {
        if (!config.fancyContributors) return false
        return when (externalMod) {
            // normal SkyHanni contributor
            null -> true

            // TODO add other mod's devs, e.g skytils

            "SBA" -> config.fancySbaContributors

            else -> false
        }
    }
}
