package at.hannibal2.skyhanni.data.jsonobjects.local

import com.google.gson.annotations.Expose
import java.util.UUID


data class FriendsJson(
    @Expose
    val players: MutableMap<UUID, PlayerFriends> = mutableMapOf(),
)

data class PlayerFriends(
    @Expose
    val friends: MutableMap<UUID, Friend> = mutableMapOf(),
)

data class Friend(
    @Expose
    val name: String = "",

    @Expose
    var bestFriend: Boolean = false,
)
