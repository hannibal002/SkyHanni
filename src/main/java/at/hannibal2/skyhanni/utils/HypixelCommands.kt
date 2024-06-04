package at.hannibal2.skyhanni.utils

object HypixelCommands {
    fun skyblock() {
        send("skyblock")
    }

    fun bazaar(searchTerm: String) {
        send("bz $searchTerm")
    }

    fun playtime() {
        send("playtime")
    }

    fun skills() {
        send("skills")
    }

    fun viewRecipe(itemName: String) {
        send("viewrecipe $itemName")
    }

    fun recipe(itemName: String) {
        send("recipe $itemName")
    }

    fun warp(warp: String) {
        send("warp $warp")
    }

    fun island() {
        send("is")
    }

    fun gardenDesk() {
        send("desk")
    }

    fun teleportToPlot(plotName: String) {
        send("tptoplot $plotName")
    }

    fun gardenLevels() {
        send("gardenlevels")
    }

    fun setHome() {
        send("sethome")
    }

    fun getFromSacks(itemName: String, amount: Int) {
        val realItemName = when (itemName) {
            // These two are a workaround for a bug with NEU's item lookup.
            // (The incorrect items cannot go in sacks anyway.)
            "BUILDER_BROWN_MUSHROOM" -> "BROWN_MUSHROOM"
            "HAY_BALE" -> "HAY_BLOCK"

            // This one is a workaround for Hypixel's /gfs behavior of taking
            // both item names and internal IDs causing a collision.
            // (Actual sulphur will get mapped to SULPHUR_ORE.)
            "SULPHUR" -> "GUNPOWDER"

            else -> itemName
        }
        send("gfs $realItemName $amount")
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

    fun calendar() {
        send("calendar")
    }

    fun sacks() {
        send("sax")
    }

    fun toggleMusic() {
        send("togglemusic")
    }

    fun bingo() {
        send("bingo")
    }

    fun wiki(text: String) {
        send("wiki $text")
    }

    fun partyWarp() {
        send("party warp")
    }

    fun partyTransfer(player: String) {
        send("party transfer $player")
    }

    fun partyDisband() {
        send("party disband")
    }

    fun partyKick(player: String) {
        send("party kick $player")
    }

    fun partyKickOffline() {
        send("party kickoffline")
    }

    fun partyAllInvite() {
        send("party settings allinvite")
    }

    fun partyPromote(player: String) {
        send("party promote $player")
    }

    fun partyChat(message: String) {
        send("pc $message")
    }

    fun allChat(message: String) {
        send("ac $message")
    }

    fun particleQuality(quality: String) {
        send("pq $quality")
    }

    fun showRng(major: String? = null, minor: String? = null) = when {
        major == null || minor == null -> send("rng")
        else -> send("rng $major $minor")
    }

    fun chatPrompt(prompt: String) {
        send("chatprompt $prompt")
    }

    private fun send(command: String) {
        @Suppress("DEPRECATION")
        // TODO rename function
        ChatUtils.sendCommandToServer(command)
    }
}
