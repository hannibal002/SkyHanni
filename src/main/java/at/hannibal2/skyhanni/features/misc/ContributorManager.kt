package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorJsonEntry
import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorsJson
import at.hannibal2.skyhanni.data.mob.MobFilter.isRealPlayer
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.entity.EntityDisplayNameEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object ContributorManager {
    private val config get() = SkyHanniMod.feature.dev

    private var contributors: Map<String, ContributorJsonEntry> = emptyMap()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        contributors = event.getConstant<ContributorsJson>("Contributors").contributors.mapKeys { it.key.lowercase() }
    }

    @SubscribeEvent
    fun onRenderNametag(event: EntityDisplayNameEvent) {
        if (!config.contributorNametags) return
        (event.entity as? EntityPlayer)?.let { player ->
            if (player.isRealPlayer()) getSuffix(event.entity.name)?.let {
                event.chatComponent.appendSibling(ChatComponentText(" $it"))
            }
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
            // normal SkyHanni contributor
            null -> true

            // TODO add other mod's devs, e.g skytils

            "SBA" -> config.fancySbaContributors

            else -> false
        }
    }
}
