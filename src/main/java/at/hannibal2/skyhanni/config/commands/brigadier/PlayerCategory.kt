package at.hannibal2.skyhanni.config.commands.brigadier

import at.hannibal2.skyhanni.data.FriendApi
import at.hannibal2.skyhanni.data.GuildApi
import at.hannibal2.skyhanni.data.PartyApi
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.PlayerUtils

enum class PlayerCategory(private val usernamesGetter: () -> Sequence<String>) {
    ISLAND_PLAYERS({ EntityUtils.getPlayerEntities().asSequence().map { it.name.string } }),
    SELF({ sequenceOf(PlayerUtils.getName()) }),
    PARTY({ PartyApi.partyMembers.asSequence() }),
    GUILD({ GuildApi.getAllMembers().asSequence() }),
    FRIENDS({ FriendApi.getAllFriends().asSequence().map { it.name } }),
    ;

    fun usernames(): Sequence<String> = usernamesGetter()
}

