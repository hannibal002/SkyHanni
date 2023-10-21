package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchRegex
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class ChatFilter {
    private val config get() = SkyHanniMod.feature.chat.filterType

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        val blockReason = block(event.message)
        if (blockReason != "") {
            event.blockedReason = blockReason
        }
    }

    private fun block(message: String): String = when {
        lobby(message) && config.hypixelHub -> "lobby"
        empty(message) && config.empty -> "empty"
        warping(message) && config.warping -> "warping"
        welcome(message) && config.welcome -> "welcome"
        isGuildExp(message) && config.guildExp -> "guild_exp"
        killCombo(message) && config.killCombo -> "kill_combo"
        profileJoin(message) && config.profileJoin -> "profile_join"

        bazaarAndAHMiniMessages(message) && config.others -> "bz_ah_minis"
        slayer(message) && config.others -> "slayer"
        slayerDrop(message) && config.others -> "slayer_drop"
        uselessDrop(message) && config.others -> "useless_drop"
        uselessNotification(message) && config.others -> "useless_notification"
        party(message) && config.others -> "party"
        money(message) && config.others -> "money"
        winterIsland(message) && config.others -> "winter_island"
        uselessWarning(message) && config.others -> "useless_warning"
        annoyingSpam(message) && config.others -> "annoying_spam"

        isWinterGift(message) && config.winterGift -> "winter_gift"
        isPowderMining(message) && config.powderMining -> "powder_mining"


        else -> ""
    }

    //TODO split into others
    private fun annoyingSpam(message: String): Boolean {
        if (message.matchRegex("§7Your Implosion hit (.*) for §r§c(.*) §r§7damage.")) return true
        if (message.matchRegex("§7Your Molten Wave hit (.*) for §r§c(.*) §r§7damage.")) return true
        if (message == "§cThere are blocks in the way!") return true
        if (message == "§aYour Blessing enchant got you double drops!") return true
        if (message == "§cYou can't use the wardrobe in combat!") return true
        if (message == "§6§lGOOD CATCH! §r§bYou found a §r§fFish Bait§r§b.") return true
        if (message == "§6§lGOOD CATCH! §r§bYou found a §r§aGrand Experience Bottle§r§b.") return true
        if (message == "§6§lGOOD CATCH! §r§bYou found a §r§aBlessed Bait§r§b.") return true
        if (message == "§6§lGOOD CATCH! §r§bYou found a §r§fDark Bait§r§b.") return true
        if (message == "§6§lGOOD CATCH! §r§bYou found a §r§fLight Bait§r§b.") return true
        if (message == "§6§lGOOD CATCH! §r§bYou found a §r§aHot Bait§r§b.") return true
        if (message == "§6§lGOOD CATCH! §r§bYou found a §r§fSpooky Bait§r§b.") return true

        return false
    }

    private fun uselessNotification(message: String): Boolean {
        if (message.matchRegex("§aYou tipped (\\d+) (player|players)!")) return true

        return when (message) {
            "§eYour previous §r§6Plasmaflux Power Orb §r§ewas removed!" -> true
            "§aYou used your §r§6Mining Speed Boost §r§aPickaxe Ability!" -> true
            "§cYour Mining Speed Boost has expired!" -> true
            "§a§r§6Mining Speed Boost §r§ais now available!" -> true
            else -> false
        }
    }

    private fun uselessWarning(message: String) = when {
        message == "§cYou are sending commands too fast! Please slow down." -> true//TODO prevent in the future
        message == "§cYou can't use this while in combat!" -> true
        message == "§cYou can not modify your equipped armor set!" -> true
        message == "§cPlease wait a few seconds between refreshing!" -> true
        message == "§cThis item is not salvageable!" -> true//prevent in the future
        message == "§cPlace a Dungeon weapon or armor piece above the anvil to salvage it!" -> true
        message == "§cWhoa! Slow down there!" -> true
        message == "§cWait a moment before confirming!" -> true
        message == "§cYou cannot open the SkyBlock menu while in combat!" -> true

        else -> false
    }

    private fun uselessDrop(message: String): Boolean {
        when {
            // TODO check if this is still necessary
            message.matchRegex("§6§lRARE DROP! §r§aEnchanted Ender Pearl (.*)") -> return true
            message == "§6§lRARE DROP! §r§aEnchanted Ender Pearl" -> return true
            message == "§6§lRARE DROP! §r§aEnchanted End Stone" -> return true
            message == "§6§lRARE DROP! §r§5Crystal Fragment" -> return true

            message.matchRegex("§6§lRARE DROP! §r§fCarrot (.*)") -> return true
            message.matchRegex("§6§lRARE DROP! §r§fPotato (.*)") -> return true

            message.matchRegex("§6§lRARE DROP! §r§9Machine Gun Bow (.*)") -> return true
            message.matchRegex("§6§lRARE DROP! §r§5Earth Shard (.*)") -> return true
            message.matchRegex("§6§lRARE DROP! §r§5Zombie Lord Chestplate (.*)") -> return true
        }

        return false
    }

    private fun winterIsland(message: String) = when {
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
        if (message == "§9§m-----------------------------------------------------") return true

        return false
    }

    private fun slayerDrop(message: String): Boolean {
        //Zombie
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
        if (message.matchRegex("§5§lVERY RARE DROP!  §r§7\\(§r§f§r§2◆ Pestilence Rune I§r§7\\) §r§b(.*)")) return true

        //Tarantula
        if (message.matchRegex("§6§lRARE DROP! §r§9Arachne's Keeper Fragment (.+)")) return true
        if (message.matchRegex("§6§lRARE DROP! §r§5Travel Scroll to Spider's Den Top of Nest (.+)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§f§r§a◆ Bite Rune I§r§7\\) (.+)")) return true
        if (message.matchRegex("§b§lRARE DROP! §r§7\\(§r§f§r§7(.+)x §r§f§r§aToxic Arrow Poison§r§7\\) (.+)")) return true
        if (message.matchRegex("§b§lRARE DROP! §r§7\\(§r§f§r§aToxic Arrow Poison§r§7\\) (.+)")) return true
        if (message.matchRegex("§5§lVERY RARE DROP!  §r§7\\(§r§9Bane of Arthropods VI§r§7\\) (.+)")) return true

        //Enderman
        if (message.matchRegex("§b§lRARE DROP! §r§7\\(§r§f§r§7(.*)x §r§f§r§aTwilight Arrow Poison§r§7\\) (.*)")) return true
        if (message.matchRegex("§5§lVERY RARE DROP!  §r§7\\(§r§fMana Steal I§r§7\\) (.*)")) return true
        if (message.matchRegex("§5§lVERY RARE DROP!  §r§7\\(§r§f§r§5Sinful Dice§r§7\\) (.*)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§f§r§9Null Atom§r§7\\) (.*)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§f§r§5Transmission Tuner§r§7\\) (.*)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§fMana Steal I§r§7\\) (.*)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§f§r§5◆ Endersnake Rune I§r§7\\) (.*)")) return true
        if (message.matchRegex("§d§lCRAZY RARE DROP!  §r§7\\(§r§f§r§fPocket Espresso Machine§r§7\\) (.*)")) return true
        if (message.matchRegex("§5§lVERY RARE DROP!  §r§7\\(§r§f§r§5◆ End Rune I§r§7\\) (.*)")) return true
        if (message.matchRegex("§5§lVERY RARE DROP!  §r§7\\(§r§f§r§6Hazmat Enderman§r§7\\) .*")) return true

        //Blaze
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§f§r§fWisp's Ice-Flavored Water I Splash Potion§r§7\\) (.*)")) return true
        if (message.matchRegex("§b§lRARE DROP! §r§7\\(§r§f§r§5Bundle of Magma Arrows§r§7\\) (.*)")) return true
        if (message.matchRegex("§9§lVERY RARE DROP!  §r§7\\(§r§f§r§7\\d+x §r§f§r§9(Glowstone|Blaze Rod|Magma Cream|Nether Wart) Distillate§r§7\\) (.*)")) return true

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

        if (message.startsWith("§e✆ RING... ")) return true

        return false
    }

    private fun profileJoin(message: String) = when {
        message.startsWith("§aYou are playing on profile: §e") -> true
        message.startsWith("§8Profile ID: ") -> true

        else -> false
    }

    private fun bazaarAndAHMiniMessages(message: String) = when (message) {
        "§7Putting item in escrow...",
        "§7Putting coins in escrow...",

            //Auction House
        "§7Setting up the auction...",
        "§7Processing purchase...",
        "§7Processing bid...",
        "§7Claiming BIN auction...",

            //Bazaar
        "§6[Bazaar] §r§7Submitting sell offer...",
        "§6[Bazaar] §r§7Submitting buy order...",
        "§6[Bazaar] §r§7Executing instant sell...",
        "§6[Bazaar] §r§7Executing instant buy...",
        "§6[Bazaar] §r§7Cancelling order...",
        "§6[Bazaar] §r§7Claiming order...",
        "§6[Bazaar] §r§7Putting goods in escrow...",

            //Bank
        "§8Depositing coins...",
        "§8Withdrawing coins...",
        -> true

        else -> false
    }

    private fun killCombo(message: String): Boolean {
        //§a§l+5 Kill Combo §r§8+§r§b3% §r§b? Magic Find
        return when {
            message.matchRegex("§.§l\\+(.*) Kill Combo (.*)") -> true
            message == "§6§l+50 Kill Combo" -> true
            message.matchRegex("§cYour Kill Combo has expired! You reached a (.*) Kill Combo!") -> true
            else -> false
        }
    }

    private fun lobby(message: String) = when {
        //player join
        message.matchRegex("(?: §b>§c>§a>§r §r)?.* §6(?:joined|(?:spooked|slid) into) the lobby!(?:§r §a<§c<§b<)?") -> true

        //mystery box
        message.matchRegex("§b✦ §r.* §r§7found a §r§e.* §r§bMystery Box§r§7!") -> true
        message.matchRegex("§b✦ §r.* §r§7found (a|an) §r.* §r§7in a §r§a(Holiday )?Mystery Box§r§7!") -> true

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

    private fun isGuildExp(message: String) =
    // §aYou earned §r§22 GEXP §r§afrom playing SkyBlock!
        // §aYou earned §r§22 GEXP §r§a+ §r§c210 Event EXP §r§afrom playing SkyBlock!
        message.matchRegex("§aYou earned §r§2.* GEXP (§r§a\\+ §r§.* Event EXP )?§r§afrom playing SkyBlock!")

    private fun welcome(message: String): Boolean = message == "§eWelcome to §r§aHypixel SkyBlock§r§e!"

    private fun warping(message: String) = when {
        message.matchRegex("§7Sending to server (.*)\\.\\.\\.") -> true
        message.matchRegex("§7Request join for Hub (.*)\\.\\.\\.") -> true
        message.matchRegex("§7Request join for Dungeon Hub #(.*)\\.\\.\\.") -> true
        message == "§7Warping..." -> true
        message == "§7Warping you to your SkyBlock island..." -> true
        message == "§7Warping using transfer token..." -> true

        //visiting other players
        message == "§7Finding player..." -> true
        message == "§7Sending a visit request..." -> true

        //warp portals on public islands (canvas room – flower house, election room – community center, void sepulture – the end)
        message.matchRegex("§dWarped to (.*)§r§d!") -> true
        else -> false
    }

    private fun empty(message: String) = message.removeColor().trimWhiteSpaceAndResets().isEmpty()

    private fun isWinterGift(message: String) = when {
        //winter gifts useless
        message.matchRegex("§f§lCOMMON! §r§3.* XP §r§egift with §r.*§r§e!") -> true
        message.matchRegex("(§f§lCOMMON|§9§lRARE)! §r.* XP Boost .* Potion §r.*§r§e!") -> true
        message.matchRegex("(§f§lCOMMON|§9§lRARE)! §r§6.* coins §r§egift with §r.*§r§e!") -> true

        //enchanted book
        message.matchRegex("§9§lRARE! §r§9Scavenger IV §r§egift with §r.*§r§e!") -> true
        message.matchRegex("§9§lRARE! §r§9Looting IV §r§egift with §r.*§r§e!") -> true
        message.matchRegex("§9§lRARE! §r§9Luck VI §r§egift with §r.*§r§e!") -> true

        message.matchRegex("§e§lSWEET! §r§f(Grinch|Santa|Gingerbread Man) Minion Skin §r§egift with §r.*§r§e!") -> true
        message.matchRegex("§9§lRARE! §r§f◆ Ice Rune §r§egift with §r.*§r§e!") -> true

        //furniture
        message.matchRegex("§e§lSWEET! §r§fTall Holiday Tree §r§egift with §r.*§r§e!") -> true
        message.matchRegex("§e§lSWEET! §r§fNutcracker §r§egift with §r.*§r§e!") -> true
        message.matchRegex("§e§lSWEET! §r§fPresent Stack §r§egift with §r.*§r§e!") -> true

        message.matchRegex("§e§lSWEET! §r§9(Winter|Battle) Disc §r§egift with §r.*§r§e!") -> true

        //winter gifts a bit useful
        message.matchRegex("§e§lSWEET! §r§9Winter Sack §r§egift with §r.*§r§e!") -> true
        message.matchRegex("§e§lSWEET! §r§5Snow Suit .* §r§egift with §r.*§r§e!") -> true

        //winter gifts not your gifts
        message.matchRegex("§cThis gift is for §r.*§r§c, sorry!") -> true

        else -> false
    }

    private fun isPowderMining(message: String) = when {
        message.matchRegex("§cYou need a stronger tool to mine (Amethyst|Ruby|Jade|Amber|Sapphire|Topaz) Gemstone Block§r§c.") -> true

        message.matchRegex("§aYou received §r§f\\d* §r§f[❤❈☘⸕✎✧] §r§fRough (Ruby|Amethyst|Jade|Amber|Sapphire|Topaz) Gemstone§r§a\\.") -> true
        message.matchRegex("§aYou received §r§f\\d §r§a[❤❈☘⸕✎✧] §r§aFlawed (Ruby|Amethyst|Jade|Amber|Sapphire|Topaz) Gemstone§r§a\\.") -> true


        message == "§aYou uncovered a treasure chest!" -> true
        message == "§aYou received §r§f1 §r§aWishing Compass§r§a." -> true
        message == "§aYou received §r§f1 §r§9Ascension Rope§r§a." -> true

        //Jungle
        message.matchRegex("§aYou received §r§f\\d* §r§aSludge Juice§r§a\\.") -> true
        message == "§aYou received §r§f1 §r§aOil Barrel§r§a." -> true

        //Useful, maybe in another chat
        message.matchRegex("§aYou received §r§b\\+\\d{1,3} §r§a(Mithril|Gemstone) Powder.") -> true
        message == "§6You have successfully picked the lock on this chest!" -> true

        else -> false
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "chat.hypixelHub", "chat.filterType.hypixelHub")
        event.move(3, "chat.empty", "chat.filterType.empty")
        event.move(3, "chat.warping", "chat.filterType.warping")
        event.move(3, "chat.guildExp", "chat.filterType.guildExp")
        event.move(3, "chat.friendJoinLeft", "chat.filterType.friendJoinLeft")
        event.move(3, "chat.winterGift", "chat.filterType.winterGift")
        event.move(3, "chat.powderMining", "chat.filterType.powderMining")
        event.move(3, "chat.killCombo", "chat.filterType.killCombo")
        event.move(3, "chat.profileJoin", "chat.filterType.profileJoin")
        event.move(3, "chat.others", "chat.filterType.others")
    }
}
