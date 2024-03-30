package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.FriendAPI
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.data.jsonobjects.repo.VipVisitsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.EntityUtils.isNPC
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PlayerTabComplete {

    private val config get() = SkyHanniMod.feature.misc.commands.tabComplete
    private var vipVisits = listOf<String>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<VipVisitsJson>("VipVisits")
        vipVisits = data.vipVisits
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.tabCompleteCommands", "commands.tabComplete")
    }

    enum class PlayerCategory {
        FRIENDS,
        ISLAND_PLAYERS,
    }

    fun handleTabComplete(command: String): List<String>? {
        val commands = mapOf(
            "f" to listOf(PlayerCategory.FRIENDS),
            "friend" to listOf(PlayerCategory.FRIENDS),

            "msg" to listOf(),
            "w" to listOf(),
            "tell" to listOf(),
            "boop" to listOf(),

            "visit" to listOf(),
            "invite" to listOf(),
            "ah" to listOf(),

            "pv" to listOf(), // NEU's Profile Viewer
            "shmarkplayer" to listOf(), // SkyHanni's Mark Player
        )
        val ignored = commands[command] ?: return null

        return buildList {

            if (config.friends && PlayerCategory.FRIENDS !in ignored) {
                FriendAPI.getAllFriends().filter { it.bestFriend || !config.onlyBestFriends }
                    .forEach { add(it.name) }
            }

            if (config.islandPlayers && PlayerCategory.ISLAND_PLAYERS !in ignored) {
                for (entity in Minecraft.getMinecraft().theWorld.playerEntities) {
                    if (!entity.isNPC() && entity is EntityOtherPlayerMP) {
                        add(entity.name)
                    }
                }
            }

            if (config.party) {
                for (member in PartyAPI.partyMembers) {
                    add(member)
                }
            }

            if (config.vipVisits && command == "visit") {
                for (visit in vipVisits) {
                    add(visit)
                }
            }
        }
    }
}
