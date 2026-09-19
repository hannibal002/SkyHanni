package at.hannibal2.skyhanni.features.commands.tabcomplete

import at.hannibal2.skyhanni.data.FriendApi
import at.hannibal2.skyhanni.data.GuildApi
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.features.misc.CarryTracker
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.PlayerUtils

enum class PlayerNameSource(private val usernamesGetter: () -> List<String>) {
    ISLAND_PLAYERS({ EntityUtils.getPlayerEntities().map { it.name.string } }),
    SELF({ listOf(PlayerUtils.getName()) }),
    PARTY({ PartyApi.partyMembers }),
    GUILD({ GuildApi.getAllMembers() }),
    FRIENDS({ FriendApi.getAllFriends().map { it.name } }),
    BEST_FRIENDS({ FriendApi.getAllFriends().filter { it.bestFriend }.map { it.name } }),
    CARRY_CUSTOMER({ CarryTracker.getCustomers().map { it.name } }),
    ;

    val usernames: List<String> get() = usernamesGetter()
}
