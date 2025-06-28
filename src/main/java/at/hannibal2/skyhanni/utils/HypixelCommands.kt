package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.api.GetFromSackApi
import at.hannibal2.skyhanni.utils.ChatUtils.debug
import at.hannibal2.skyhanni.utils.ChatUtils.sendMessageToServer

@Suppress("TooManyFunctions")
object HypixelCommands {
    fun skyblock() {
        send("skyblock")
    }

    fun bazaar(searchTerm: String) {
        send("bz $searchTerm")
    }

    fun auctionSearch(searchTerm: String) {
        send("ahs $searchTerm")
    }

    fun playtime() {
        send("playtime")
    }

    fun skyblockMenu() {
        send("sbmenu")
    }

    fun skills() {
        send("skills")
    }

    fun viewRecipe(itemId: NeuInternalName, page: Int = 1) {
        send("viewrecipe ${itemId.skyblockCommandId} $page")
    }

    fun recipe(itemName: String) {
        send("recipe $itemName")
    }

    // opens the crafting table
    fun craft() {
        send("craft")
    }

    fun npcOption(npc: String, answer: String) {
        send("selectnpcoption $npc $answer")
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
        send("plottp $plotName")
    }

    fun gardenLevels() {
        send("gardenlevels")
    }

    fun setHome() {
        send("sethome")
    }

    // Do not remove this deprecation tag, as we want to catch all wrong uses of /gfs in the future forever.
    @Deprecated("do not send /gfs commands manually to hypixel", ReplaceWith("GetFromSackApi.getFromSack(internalName, amount)"))
    fun getFromSacks(internalName: NeuInternalName, amount: Int) {
        GetFromSackApi.getFromSack(internalName, amount)
    }

    fun widget() {
        send("widget")
    }

    fun chocolateFactory() {
        send("cf")
    }

    fun pet() {
        send("pet")
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

    fun backPack(position: Int) {
        send("bp $position")
    }

    fun enderChest(position: Int) {
        send("ec $position")
    }

    fun partyAccept(player: String) {
        send("party accept $player")
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

    fun partyChat(message: String, prefix: Boolean = false) {
        when (prefix) {
            false -> send("pc $message")
            true -> send("pc [SkyHanni] $message")
        }
    }

    fun partyInvite(player: String) {
        send("party $player")
    }

    fun allChat(message: String) {
        send("ac $message")
    }

    fun particleQuality(quality: String) {
        send("pq $quality")
    }

    // Changes the speed of Rancher's Boots
    fun setMaxSpeed(speed: Int? = null) = when {
        speed == null -> send("setmaxspeed")
        else -> send("setmaxspeed $speed")
    }

    fun showRng(major: String? = null, minor: String? = null) = when {
        major == null || minor == null -> send("rng")
        else -> send("rng $major $minor")
    }

    fun chatPrompt(prompt: String) {
        send("chatprompt $prompt")
    }

    fun callback(uuid: String) {
        send("cb $uuid")
    }

    fun bank() {
        send("bank")
    }

    fun pickupStash() {
        send("pickupstash")
    }

    fun viewStash(type: String) {
        send("viewstash $type")
    }

    fun locraw() {
        send("locraw")
    }

    private fun send(command: String) {
        if (command.startsWith("/")) {
            debug("Sending wrong command to server? ($command)")
        }
        sendMessageToServer("/$command")
    }
}
