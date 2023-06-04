package at.hannibal2.skyhanni.features.misc.tabcomplete

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.FriendAPI
import at.hannibal2.skyhanni.data.PartyAPI
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.jsonobjects.VipVisitsJson
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PlayerTabComplete {
    private val config get() = SkyHanniMod.feature.misc.tabCompleteCommands
    private var vipVisitsJson: VipVisitsJson? = null

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        vipVisitsJson = event.getConstant<VipVisitsJson>("VipVisits")
    }

    enum class PlayerCategory {
        PARTY,
        FRIENDS,
        ISLAND_PLAYERS,
    }

    fun handleTabComplete(command: String, originalArray: Array<String>): List<String>? {
        val commands = mapOf(
            "p" to listOf(PlayerCategory.PARTY),
            "party" to listOf(PlayerCategory.PARTY),
            "pt" to listOf(PlayerCategory.FRIENDS, PlayerCategory.ISLAND_PLAYERS), // /party transfer
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

            if (config.friends) {
                if (PlayerCategory.FRIENDS !in ignored) {
                    FriendAPI.getAllFriends().filter { it.bestFriend || !config.onlyBestFriends }
                        .forEach { add(it.name) }
                }
            }

            if (config.islandPlayers) {
                if (PlayerCategory.ISLAND_PLAYERS !in ignored) {
                    for (name in originalArray) {
                        if (name != LorenzUtils.getPlayerName()) {
                            add(name)
                        }
                    }
                }
            }

            if (config.party) {
                if (PlayerCategory.PARTY !in ignored) {
                    for (member in PartyAPI.partyMembers) {
                        add(member)
                    }
                }

            }

            if (config.vipVisits) {
                if (command == "visit") {
                    vipVisitsJson?.let {
                        for (visit in it.vipVisits) {
                            add(visit)
                        }
                    }
                }
            }
        }
    }
}