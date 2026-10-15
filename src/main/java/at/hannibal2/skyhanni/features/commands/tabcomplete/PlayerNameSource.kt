package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.data.FriendApi
import at.hannibal2.skyhanni.data.GuildApi
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.features.misc.CarryTracker
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.PlayerUtils

enum class PlayerNameSource(
    private val displayName: String,
    private val usernamesGetter: () -> List<String>,
) {
    ISLAND_PLAYERS("§aIsland Players", { EntityUtils.getPlayerEntities().map { it.gameProfile.name } }),
    SELF("§bYourself", { listOf(PlayerUtils.getName()) }),
    PARTY("§dParty Members", { PartyApi.partyMembers }),
    GUILD("§2Guild Members", { GuildApi.getAllMembers() }),
    FRIENDS("§eFriends", { FriendApi.getAllFriends().map { it.name } }),
    BEST_FRIENDS("§6Best Friends", { FriendApi.getAllFriends().filter { it.bestFriend }.map { it.name } }),
    CARRY_CUSTOMER("§cCarry Customers", { CarryTracker.getCustomers().map { it.name } }),
    ;

    val usernames: List<String> get() = usernamesGetter()

    override fun toString(): String = displayName
}
