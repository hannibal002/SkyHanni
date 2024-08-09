package at.hannibal2.skyhanni.data.hypixel.location

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
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

    val lobbyType get() = lobbyName?.let {
        lobbyTypePattern.matchMatcher(it) { group("lobbyType") }
    }

    val serverId: String get() {
        val number = serverName.indexOfFirst { it.isDigit() }
        val type = when (serverName.take(number)) {
            "dynamiclobby" -> "L"
            "mini" -> "m"
            "mega" -> "M"
            "limbo" -> "limbo"
            else -> ""
        }
        val code = serverName.substring(number)
        return "$type$code"
    }


    companion object {
        private val lobbyTypePattern by RepoPattern.pattern(
            "data.hypixel.location.lobbytype",
            "(?<lobbyType>.*lobby)\\d+",
        )
    }
}
