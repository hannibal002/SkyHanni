package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorJsonEntry
import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.mapKeysNotNull
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
        val map = event.getConstant<ContributorsJson>("ContributorList").contributors

        contributors = map.mapKeysNotNull {
            try {
                UUID.fromString(it.key)
            } catch (e: IllegalArgumentException) {
                ErrorManager.logErrorWithData(
                    e,
                    "Failed to parse contributor UUID",
                    "key" to it.key, "value" to it.value
                )
                null
            }
        }
        contributorNames = map.values.mapNotNull { it.displayName }
    }

    @HandleEvent
    fun onRenderNametag(event: EntityDisplayNameEvent<Player>) {
        if (!config.contributorNametags) return

        val gameProfile = event.entity.gameProfile
        getSuffix(gameProfile.id)?.let {
            event.chatComponent.append(it)
        }
    }

    fun getSuffix(uuid: UUID): String? = contributors[uuid]?.suffix

    fun shouldSpin(uuid: UUID): Boolean = contributors[uuid]?.spinny ?: false
    fun shouldBeUpsideDown(uuid: UUID): Boolean = contributors[uuid]?.upsideDown ?: false
}
