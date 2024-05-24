package at.hannibal2.skyhanni.utils

object HypixelCommands {

    fun bazaar(searchTerm: String) {
        send("bz $searchTerm")
    }

    fun warp(warp: String) {
        send("warp $warp")
    }

    fun teleportToPlot(plotName: String) {
        send("tptoplot $plotName")
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

    fun gardenLevels() {
        send("gardenlevels")
    }

    fun calendar() {
        send("calendar")
    }

    fun sacks() {
        send("sax")
    }

    fun toggleMusic() {
        send("togglemusic")
    }

    fun partyWarp() {
        send("party warp")
    }

    fun partyTransfer(player: String) {
        send("party transfer $player")
    }

    fun particleQuality(quality: String) {
        send("pq $quality")
    }

    fun partyAllInvite() {
        send("party settings allinvite")
    }

    fun allChat(message: String) {
        send("ac $message")
    }

    fun showRng(major: String? = null, minor: String? = null) = when {
        major == null || minor == null -> send("rng")
        else -> send("rng $major $minor")
    }

    fun island() {
        send("is")
    }

    fun hub() {
        send("hub")
    }

    private fun send(command: String) {
        @Suppress("DEPRECATION")
        // TODO rename function
        ChatUtils.sendCommandToServer(command)
    }
}
