package at.lorenz.mod.chat

import at.lorenz.mod.events.LorenzChatEvent
import at.lorenz.mod.utils.LorenzUtils
import at.lorenz.mod.utils.LorenzUtils.matchRegex
import com.thatgravyboat.skyblockhud.LorenzMod
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ChatFilter {

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!LorenzMod.feature.chat.mainFilter) return

        val blockReason = block(event.message)
        if (blockReason != "") {
            event.blockedReason = blockReason
        }
    }

    private fun block(message: String): String = when {
        message.startsWith("§aYou are playing on profile: §e") -> "profile"//TODO move into own class
        lobby(message) -> "lobby"
        empty(message) -> "empty"
        warping(message) -> "warping"
        welcome(message) -> "welcome"
        guild(message) -> "guild"
        killCombo(message) -> "kill_combo"
        bazaarAndAHMiniMessages(message) -> "bz_ah_minis"
        watchdogAnnouncement(message) -> "watchdog"
        slayer(message) -> "slayer"
        slayerDrop(message) -> "slayer_drop"
        uselessDrop(message) -> "useless_drop"
        uselessNotification(message) -> "useless_notification"
        party(message) -> "party"
        money(message) -> "money"
        winterIsland(message) -> "winter_island"
        uselessWarning(message) -> "useless_warning"
        friendJoin(message) -> "friend_join"


        else -> ""
    }

    private fun friendJoin(message: String): Boolean {
        return when {
            message.matchRegex("§aFriend > §r(.*) §r§e(joined|left).") -> {
                true
            }
            else -> false
        }

    }

    private fun uselessNotification(message: String): Boolean {
        return when {
            message == "§eYour previous §r§6Plasmaflux Power Orb §r§ewas removed!" -> true

            else -> false
        }
    }

    private fun uselessWarning(message: String): Boolean = when {
        message == "§cYou are sending commands too fast! Please slow down." -> true//TODO prevent in the future
        message == "§cYou can't use this while in combat!" -> true
        message == "§cYou can not modify your equipped armor set!" -> true
        message == "§cPlease wait a few seconds between refreshing!" -> true
        message == "§cThis item is not salvageable!" -> true//prevent in the future
        message == "§cPlace a Dungeon weapon or armor piece above the anvil to salvage it!" -> true
        message == "§cWhoa! Slow down there!" -> true
        message == "§cWait a moment before confirming!" -> true
        message == "§cYou need to be out of combat for 3 seconds before opening the SkyBlock Menu!" -> true//TODO prevent in the future

        else -> false
    }

    private fun uselessDrop(message: String): Boolean {
        when {
            message.matchRegex("§6§lRARE DROP! §r§aEnchanted Ender Pearl (.*)") -> return true

            message.matchRegex("§6§lRARE DROP! §r§fCarrot (.*)") -> return true
            message.matchRegex("§6§lRARE DROP! §r§fPotato (.*)") -> return true

            message.matchRegex("§6§lRARE DROP! §r§9Machine Gun Bow (.*)") -> return true
            message.matchRegex("§6§lRARE DROP! §r§5Earth Shard (.*)") -> return true
            message.matchRegex("§6§lRARE DROP! §r§5Zombie Lord Chestplate (.*)") -> return true
        }

        return false
    }

    private fun winterIsland(message: String): Boolean = when {
        message.matchRegex(" §r§f☃ §r§7§r(.*) §r§7mounted a §r§fSnow Cannon§r§7!") -> true

        else -> false
    }

    private fun money(message: String): Boolean {
        if (isBazaar(message)) return true
        if (isAuctionHouse(message)) return true

        return false
    }

    private fun isAuctionHouse(message: String): Boolean {
        if (message == "§b-----------------------------------------------------") return true
        if (message == "§eVisit the Auction House to collect your item!") return true

        return false
    }

    private fun isBazaar(message: String): Boolean {
        if (message.matchRegex("§eBuy Order Setup! §r§a(.*)§r§7x (.*) §r§7for §r§6(.*) coins§r§7.")) return true
        if (message.matchRegex("§eSell Offer Setup! §r§a(.*)§r§7x (.*) §r§7for §r§6(.*) coins§r§7.")) return true
        if (message.matchRegex("§cCancelled! §r§7Refunded §r§6(.*) coins §r§7from cancelling buy order!")) return true
        if (message.matchRegex("§cCancelled! §r§7Refunded §r§a(.*)§r§7x (.*) §r§7from cancelling sell offer!")) return true

        return false
    }

    private fun party(message: String): Boolean {
        if (message == "§9§m-----------------------------") return true
        if (message == "§9§m-----------------------------------------------------") return true

        return false
    }

    private fun slayerDrop(message: String): Boolean {
        //Revenant
        if (message.matchRegex("§b§lRARE DROP! §r§7\\(§r§f§r§9Revenant Viscera§r§7\\) (.*)")) return true
        if (message.matchRegex("§b§lRARE DROP! §r§7\\(§r§f§r§7(.*)x §r§f§r§9Foul Flesh§r§7\\) (.*)")) return true
        if (message.matchRegex("§b§lRARE DROP! §r§7\\(§r§f§r§9Foul Flesh§r§7\\) (.*)")) return true
        if (message.matchRegex("§6§lRARE DROP! §r§5Golden Powder (.*)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§f§r§2(.*) Pestilence Rune I§r§7\\) (.*)")) {
            LorenzUtils.debug("check regex for this blocked message!")
            return true
        }
        if (message.matchRegex("§5§lVERY RARE DROP!  §r§7\\(§r§f§r§5Revenant Catalyst§r§7\\) (.*)")) return true
        if (message.matchRegex("§5§lVERY RARE DROP!  §r§7\\(§r§f§r§9Undead Catalyst§r§7\\) (.*)")) return true

        //Enderman
        if (message.matchRegex("§b§lRARE DROP! §r§7\\(§r§f§r§7(.*)x §r§f§r§aTwilight Arrow Poison§r§7\\) (.*)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§fMana Steal I§r§7\\) (.*)")) return true
        if (message.matchRegex("§5§lVERY RARE DROP!  §r§7\\(§r§f§r§5Sinful Dice§r§7\\) (.*)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§f§r§9Null Atom§r§7\\) (.*)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§f§r§5Transmission Tuner§r§7\\) (.*)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§fMana Steal I§r§7\\) (.*)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§f§r§5◆ Endersnake Rune I§r§7\\) (.*)")) return true
        if (message.matchRegex("§d§lCRAZY RARE DROP!  §r§7\\(§r§f§r§fPocket Espresso Machine§r§7\\) (.*)")) return true
        if (message.matchRegex("§5§lVERY RARE DROP!  §r§7\\(§r§f§r§5◆ End Rune I§r§7\\) (.*)")) return true

        return false
    }

    private fun slayer(message: String): Boolean {
        //start
        if (message.matchRegex("  §r§5§lSLAYER QUEST STARTED!")) return true
        if (message.matchRegex("   §5§l» §7Slay §c(.*) Combat XP §7worth of (.*)§7.")) return true

        //end
        if (message.matchRegex("  §r§a§lSLAYER QUEST COMPLETE!")) return true
        if (message == "  §r§6§lNICE! SLAYER BOSS SLAIN!") return true
        if (message.matchRegex("   §r§e(.*)Slayer LVL 9 §r§5- §r§a§lLVL MAXED OUT!")) return true
        if (message.matchRegex("   §r§5§l» §r§7Talk to Maddox to claim your (.*) Slayer XP!")) return true


        if (message == "§eYou received kill credit for assisting on a slayer miniboss!") return true

        if (message == "§e✆ Ring... ") return true
        if (message == "§e✆ Ring... Ring... ") return true
        if (message == "§e✆ Ring... Ring... Ring... ") return true

        return false
    }

    private fun watchdogAnnouncement(message: String): Boolean = when {
        message == "§4[WATCHDOG ANNOUNCEMENT]" -> true
        message.matchRegex("§fWatchdog has banned §r§c§l(.*)§r§f players in the last 7 days.") -> true
        message.matchRegex("§fStaff have banned an additional §r§c§l(.*)§r§f in the last 7 days.") -> true
        message == "§cBlacklisted modifications are a bannable offense!" -> true
        else -> false
    }

    private fun bazaarAndAHMiniMessages(message: String): Boolean = when (message) {
        "§7Putting item in escrow...",
        "§7Putting goods in escrow...",
        "§7Putting coins in escrow...",

            //Auction House
        "§7Setting up the auction...",
        "§7Processing purchase...",
        "§7Claiming order...",
        "§7Processing bid...",
        "§7Claiming BIN auction...",

            //Bazaar
        "§7Submitting sell offer...",
        "§7Submitting buy order...",
        "§7Executing instant sell...",
        "§7Executing instant buy...",

            //Bank
        "§8Depositing coins...",
        "§8Withdrawing coins..." -> true
        else -> false
    }

    private fun killCombo(message: String): Boolean {
        //§a§l+5 Kill Combo §r§8+§r§b3% §r§b? Magic Find
        return when {
            message.matchRegex("§.§l\\+(.*) Kill Combo §r§8\\+(.*)") -> true
            message.matchRegex("§cYour Kill Combo has expired! You reached a (.*) Kill Combo!") -> true
            else -> false
        }
    }

    private fun lobby(message: String): Boolean = when {
        //player join
        message.matchRegex("(.*) §6joined the lobby!") -> true
        message.matchRegex(" §b>§c>§a>§r (.*) §6joined the lobby!§r §a<§c<§b<") -> true

        //mystery box
        message.matchRegex("§b✦ §r(.*) §r§7found a §r§e(.*) §r§bMystery Box§r§7!") -> true
        message.matchRegex("§b✦ §r(.*) §r§7found (a|an) §r(.*) §r§7in a §r§aMystery Box§r§7!") -> true

        //prototype
        message.contains("§r§6§lWelcome to the Prototype Lobby§r") -> true
        message == "  §r§f§l➤ §r§6You have reached your Hype limit! Add Hype to Prototype Lobby minigames by right-clicking with the Hype Diamond!" -> true

        //hypixel tournament notifications
        message.contains("§r§e§6§lHYPIXEL§e is hosting a §b§lBED WARS DOUBLES§e tournament!") -> true
        message.contains("§r§e§6§lHYPIXEL BED WARS DOUBLES§e tournament is live!") -> true

        //other
        message.contains("§aYou are still radiating with §bGenerosity§r§a!") -> true
        else -> false
    }

    private fun guild(message: String): Boolean = when {
        message.matchRegex("§2Guild > (.*) §r§e(joined|left).") -> true
        message.matchRegex("§aYou earned §r§2(.*) GEXP §r§afrom playing SkyBlock!") -> true
        message.matchRegex("§aYou earned §r§2(.*) GEXP §r§a\\+ §r§e(.*) Event EXP §r§afrom playing SkyBlock!") -> true
        message == "§b§m-----------------------------------------------------" -> true
        else -> false
    }

    private fun welcome(message: String): Boolean = message == "§eWelcome to §r§aHypixel SkyBlock§r§e!"

    private fun warping(message: String): Boolean = when {
        message.matchRegex("§7Sending to server (.*)\\.\\.\\.") -> true
        message.matchRegex("§7Request join for Hub (.*)\\.\\.\\.") -> true
        message.matchRegex("§7Request join for Dungeon Hub #(.*)\\.\\.\\.") -> true
        message == "§7Warping..." -> true
        message == "§7Warping you to your SkyBlock island..." -> true
        message == "§7Warping using transfer token..." -> true

        //visiting other players
        message == "§7Finding player..." -> true
        message == "§7Sending a visit request..." -> true

        //warp portals on public islands (canvas room - flower house, election room - community center, void sepulture - the end)
        message.matchRegex("§dWarped to (.*)§r§d!") -> true
        else -> false
    }

    private fun empty(message: String): Boolean = when (message) {
        "§8 §r§8 §r§1 §r§3 §r§3 §r§7 §r§8 ",

        "§f §r§f §r§1 §r§0 §r§2 §r§4§r§f §r§f §r§2 §r§0 §r§4 §r§8§r§0§r§1§r§0§r§1§r§2§r§f§r§f§r§0§r§1§r§3§r§4§r§f§r§f§r§0§r§1§r§5§r§f§r§f§r§0§r§1§r§6§r§f§r§f§r§0§r§1§r§8§r§9§r§a§r§b§r§f§r§f§r§0§r§1§r§7§r§f§r§f§r§3 §r§9 §r§2 §r§0 §r§0 §r§1§r§3 §r§9 §r§2 §r§0 §r§0 §r§2§r§3 §r§9 §r§2 §r§0 §r§0 §r§3§r§0§r§0§r§1§r§f§r§e§r§0§r§0§r§2§r§f§r§e§r§0§r§0§r§3§r§4§r§5§r§6§r§7§r§8§r§f§r§e§r§3 §r§6 §r§3 §r§6 §r§3 §r§6 §r§e§r§3 §r§6 §r§3 §r§6 §r§3 §r§6 §r§d",

        "§f §r§r§r§f §r§r§r§1 §r§r§r§0 §r§r§r§2 §r§r§r§f §r§r§r§f §r§r§r§2 §r§r§r§0 §r§r§r§4 §r§r§r§3 §r§r§r§9 §r§r§r§2 §r§r§r§0 §r§r§r§0 §r§r§r§3 §r§r§r§9 §r§r§r§2 §r§r§r§0 §r§r§r§0 §r§r§r§3 §r§r§r§9 §r§r§r§2 §r§r§r§0 §r§r§r§0 ",

        "",
        "§f",
        "§c" -> true
        else -> false
    }
}
