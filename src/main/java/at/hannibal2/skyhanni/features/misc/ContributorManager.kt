package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorJsonEntry
import at.hannibal2.skyhanni.data.jsonobjects.repo.ContributorsJson
import at.hannibal2.skyhanni.events.EntityRenderNametagEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object ContributorManager {
    private val config get() = SkyHanniMod.feature.dev

    private var contributors: Map<String, ContributorJsonEntry> = emptyMap()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        contributors = event.getConstant<ContributorsJson>("Contributors").contributors.mapKeys { it.key.lowercase() }
    }

    @SubscribeEvent
    fun onRenderNametag(event: EntityRenderNametagEvent) {
        if (event.entity !is EntityPlayer) return
        getSuffix(event.entity.name)?.let {
            event.chatComponent.appendSibling(ChatComponentText(" $it"))
        }
    }

    private fun addSuffixToNametag(entity: EntityLivingBase) {
        getSuffix(entity.name)?.let { (entity as? EntityPlayer)?.addSuffix(ChatComponentText(" $it")) }
    }

    fun getSuffix(username: String): String? = getContributor(username)?.suffix

    fun canSpin(username: String): Boolean = getContributor(username)?.spinny ?: false

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
