package at.hannibal2.skyhanni.utils

object HypixelCommands {

    fun bazaar(searchTerm: String) {
        send("bz $searchTerm")
    }

    fun getFromSacks(itemName: String, amount: Int) {
        send("gfs $itemName $amount")
    }

    fun widget() {
        send("widget")
    }

    fun chocolateFactory() {
        send("cf")
    }

    fun openBaker() {
        send("openbaker")
    }

    private fun send(command: String) {
        ChatUtils.sendCommandToServer(command)
    }
}
