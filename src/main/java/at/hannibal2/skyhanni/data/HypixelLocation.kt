package at.hannibal2.skyhanni.data

import net.hypixel.data.type.ServerType

class HypixelLocation(
    val serverName: String,
    val serverType: ServerType?,
    val lobbyName: String?,
    val mode: String?,
    val map: String?
) {

    fun isLimbo() = serverName == "limbo"
    fun isLobby() = lobbyName != null


}
