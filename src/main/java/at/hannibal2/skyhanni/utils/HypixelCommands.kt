package at.hannibal2.skyhanni.utils

object HypixelCommands {

    fun bazaar(searchTerm: String) {
        ChatUtils.sendCommandToServer("bz $searchTerm")
    }

    fun getFromSacks(itemName: String, amount: Int) {
        ChatUtils.sendCommandToServer("gfs $itemName $amount")
    }

    fun widget() {
        ChatUtils.sendCommandToServer("widget")
    }
}
