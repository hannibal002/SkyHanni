package at.hannibal2.skyhanni.utils

object HypixelCommands {

    fun bazaar(searchTerm: String) {
        ChatUtils.sendCommandToServer("bz $searchTerm")
    }
}
