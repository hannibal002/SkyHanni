package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.trimWhiteSpaceAndResets
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern

class ChatFilter {
    private val config get() = SkyHanniMod.feature.chat.filterType

    /// <editor-fold desc="Regex Patterns & Messages">
    // Lobby Messages
    private val lobbyPatterns = listOf(
        // player join
        "(?: §b>§c>§a>§r §r)?.* §6(?:joined|(?:spooked|slid) into) the lobby!(?:§r §a<§c<§b<)?".toPattern(),

        // mystery box
        "§b✦ §r.* §r§7found a §r§e.* §r§bMystery Box§r§7!".toPattern(),
        "§b✦ §r.* §r§7found (a|an) §r.* §r§7in a §r§a(Holiday )?Mystery Box§r§7!".toPattern()
    )

    private val lobbyMessages = listOf(
        // prototype
        "  §r§f§l➤ §r§6You have reached your Hype limit! Add Hype to Prototype Lobby minigames by right-clicking with the Hype Diamond!"
    )
    private val lobbyMessagesContains = listOf(
        // prototype
        "§r§6§lWelcome to the Prototype Lobby§r",

        // hypixel tournament notifications
        "§r§e§6§lHYPIXEL§e is hosting a §b§lBED WARS DOUBLES§e tournament!",
        "§r§e§6§lHYPIXEL BED WARS DOUBLES§e tournament is live!",

        // other
        "§aYou are still radiating with §bGenerosity§r§a!"
    )

    // Warping
    private val warpingPatterns = listOf(
        "§7Sending to server (.*)\\.\\.\\.".toPattern(),
        "§7Request join for Hub (.*)\\.\\.\\.".toPattern(),
        "§7Request join for Dungeon Hub #(.*)\\.\\.\\.".toPattern(),
        // warp portals on public islands
        // (canvas room – flower house, election room – community center, void sepulchre – the end)
        "§dWarped to (.*)§r§d!".toPattern()
    )
    private val warpingMessages = listOf(
        "§7Warping...", "§7Warping you to your SkyBlock island...", "§7Warping using transfer token...",

        // visiting other players
        "§7Finding player...", "§7Sending a visit request..."
    )

    // Welcome
    private val welcomeMessages = listOf(
        "§eWelcome to §r§aHypixel SkyBlock§r§e!"
    )

    // Guild EXP
    private val guildExpPatterns = listOf(
        // §aYou earned §r§22 GEXP §r§afrom playing SkyBlock!
        // §aYou earned §r§22 GEXP §r§a+ §r§c210 Event EXP §r§afrom playing SkyBlock!
        "§aYou earned §r§2.* GEXP (§r§a\\+ §r§.* Event EXP )?§r§afrom playing SkyBlock!".toPattern()
    )

    // Kill Combo
    private val killComboPatterns = listOf(
        //§a§l+5 Kill Combo §r§8+§r§b3% §r§b? Magic Find
        "§.§l\\+(.*) Kill Combo (.*)".toPattern(),
        "§cYour Kill Combo has expired! You reached a (.*) Kill Combo!".toPattern()
    )
    private val killComboMessages = listOf(
        "§6§l+50 Kill Combo"
    )

    // Profile Join
    private val profileJoinMessageStartsWith = listOf(
        "§aYou are playing on profile: §e", "§8Profile ID: "
    )

    // OTHERS
    // Bazaar And AH Mini
    private val miniBazaarAndAHMessages = listOf(
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
        "§8Withdrawing coins..."
    )

    // Slayer
    private val slayerPatterns = listOf(
        //start
        " {2}§r§5§lSLAYER QUEST STARTED!".toPattern(),
        " {3}§5§l» §7Slay §c(.*) Combat XP §7worth of (.*)§7.".toPattern(),

        //end
        " {2}§r§a§lSLAYER QUEST COMPLETE!".toPattern(),
        " {3}§r§e(.*)Slayer LVL 9 §r§5- §r§a§lLVL MAXED OUT!".toPattern(),
        " {3}§r§5§l» §r§7Talk to Maddox to claim your (.*) Slayer XP!".toPattern()
    )
    private val slayerMessages = listOf(
        "  §r§6§lNICE! SLAYER BOSS SLAIN!", "§eYou received kill credit for assisting on a slayer miniboss!"
    )
    private val slayerMessageStartWith = listOf(
        "§e✆ RING... "
    )

    // Slayer Drop
    private val slayerDropPatterns = listOf(
        //Zombie
        "§b§lRARE DROP! §r§7\\(§r§f§r§9Revenant Viscera§r§7\\) (.*)".toPattern(),
        "§b§lRARE DROP! §r§7\\(§r§f§r§7(.*)x §r§f§r§9Foul Flesh§r§7\\) (.*)".toPattern(),
        "§b§lRARE DROP! §r§7\\(§r§f§r§9Foul Flesh§r§7\\) (.*)".toPattern(),
        "§6§lRARE DROP! §r§5Golden Powder (.*)".toPattern(),
        "§9§lVERY RARE DROP! {2}§r§7\\(§r§f§r§2(.*) Pestilence Rune I§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! {2}§r§7\\(§r§f§r§5Revenant Catalyst§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! {2}§r§7\\(§r§f§r§9Undead Catalyst§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! {2}§r§7\\(§r§f§r§2◆ Pestilence Rune I§r§7\\) §r§b(.*)".toPattern(),

        //Tarantula
        "§6§lRARE DROP! §r§9Arachne's Keeper Fragment (.+)".toPattern(),
        "§6§lRARE DROP! §r§5Travel Scroll to Spider's Den Top of Nest (.+)".toPattern(),
        "§9§lVERY RARE DROP! {2}§r§7\\(§r§f§r§a◆ Bite Rune I§r§7\\) (.+)".toPattern(),
        "§b§lRARE DROP! §r§7\\(§r§f§r§7(.+)x §r§f§r§aToxic Arrow Poison§r§7\\) (.+)".toPattern(),
        "§b§lRARE DROP! §r§7\\(§r§f§r§aToxic Arrow Poison§r§7\\) (.+)".toPattern(),
        "§5§lVERY RARE DROP! {2}§r§7\\(§r§9Bane of Arthropods VI§r§7\\) (.+)".toPattern(),

        //Enderman
        "§b§lRARE DROP! §r§7\\(§r§f§r§7(.*)x §r§f§r§aTwilight Arrow Poison§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! {2}§r§7\\(§r§fMana Steal I§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! {2}§r§7\\(§r§f§r§5Sinful Dice§r§7\\) (.*)".toPattern(),
        "§9§lVERY RARE DROP! {2}§r§7\\(§r§f§r§9Null Atom§r§7\\) (.*)".toPattern(),
        "§9§lVERY RARE DROP! {2}§r§7\\(§r§f§r§5Transmission Tuner§r§7\\) (.*)".toPattern(),
        "§9§lVERY RARE DROP! {2}§r§7\\(§r§fMana Steal I§r§7\\) (.*)".toPattern(),
        "§9§lVERY RARE DROP! {2}§r§7\\(§r§f§r§5◆ Endersnake Rune I§r§7\\) (.*)".toPattern(),
        "§d§lCRAZY RARE DROP! {2}§r§7\\(§r§f§r§fPocket Espresso Machine§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! {2}§r§7\\(§r§f§r§5◆ End Rune I§r§7\\) (.*)".toPattern(),
        "§5§lVERY RARE DROP! {2}§r§7\\(§r§f§r§6Hazmat Enderman§r§7\\) .*".toPattern(),

        //Blaze
        "§9§lVERY RARE DROP! {2}§r§7\\(§r§f§r§fWisp's Ice-Flavored Water I Splash Potion§r§7\\) (.*)".toPattern(),
        "§b§lRARE DROP! §r§7\\(§r§f§r§5Bundle of Magma Arrows§r§7\\) (.*)".toPattern(),
        "§9§lVERY RARE DROP! {2}§r§7\\(§r§f§r§7\\d+x §r§f§r§9(Glowstone|Blaze Rod|Magma Cream|Nether Wart) Distillate§r§7\\) (.*)".toPattern()
    )

    // Useless Drop
    private val uselessDropPatterns = listOf(
        "§6§lRARE DROP! §r§aEnchanted Ender Pearl (.*)".toPattern(),
        "§6§lRARE DROP! §r§fCarrot (.*)".toPattern(),
        "§6§lRARE DROP! §r§fPotato (.*)".toPattern(),
        "§6§lRARE DROP! §r§9Machine Gun Bow (.*)".toPattern(),
        "§6§lRARE DROP! §r§5Earth Shard (.*)".toPattern(),
        "§6§lRARE DROP! §r§5Zombie Lord Chestplate (.*)".toPattern()
    )
    private val uselessDropMessages = listOf(
        "§6§lRARE DROP! §r§aEnchanted Ender Pearl",
        "§6§lRARE DROP! §r§aEnchanted End Stone",
        "§6§lRARE DROP! §r§5Crystal Fragment",
    )

    // Useless Notification
    private val uselessNotificationPatterns = listOf(
        "§aYou tipped (\\d+) (player|players)!".toPattern()
    )
    private val uselessNotificationMessages = listOf(
        "§eYour previous §r§6Plasmaflux Power Orb §r§ewas removed!",
        "§aYou used your §r§6Mining Speed Boost §r§aPickaxe Ability!",
        "§cYour Mining Speed Boost has expired!",
        "§a§r§6Mining Speed Boost §r§ais now available!",
    )

    // Party
    private val partyMessages = listOf(
        "§9§m-----------------------------------------------------",
    )

    // MONEY
    // Auction House
    private val auctionHouseMessages = listOf(
        "§b-----------------------------------------------------", "§eVisit the Auction House to collect your item!",
    )

    // Bazaar
    private val bazaarPatterns = listOf(
        "§eBuy Order Setup! §r§a(.*)§r§7x (.*) §r§7for §r§6(.*) coins§r§7.".toPattern(),
        "§eSell Offer Setup! §r§a(.*)§r§7x (.*) §r§7for §r§6(.*) coins§r§7.".toPattern(),
        "§cCancelled! §r§7Refunded §r§6(.*) coins §r§7from cancelling buy order!".toPattern(),
        "§cCancelled! §r§7Refunded §r§a(.*)§r§7x (.*) §r§7from cancelling sell offer!".toPattern(),
    )

    // Winter Island
    private val winterIslandPatterns = listOf(
        "§r§f☃ §r§7§r(.*) §r§7mounted a §r§fSnow Cannon§r§7!".toPattern(),
    )

    // Useless Warning
    private val uselessWarningMessages = listOf(
        "§cYou are sending commands too fast! Please slow down.", // TODO prevent in the future
        "§cYou can't use this while in combat!",
        "§cYou can not modify your equipped armor set!",
        "§cPlease wait a few seconds between refreshing!",
        "§cThis item is not salvageable!", // TODO prevent in the future
        "§cPlace a Dungeon weapon or armor piece above the anvil to salvage it!",
        "§cWhoa! Slow down there!",
        "§cWait a moment before confirming!",
        "§cYou cannot open the SkyBlock menu while in combat!",
    )

    // Annoying Spam
    private val annoyingSpamPatterns = listOf(
        "§7Your Implosion hit (.*) for §r§c(.*) §r§7damage.".toPattern(),
        "§7Your Molten Wave hit (.*) for §r§c(.*) §r§7damage.".toPattern(),
    )
    private val annoyingSpamMessages = listOf(
        "§cThere are blocks in the way!",
        "§aYour Blessing enchant got you double drops!",
        "§cYou can't use the wardrobe in combat!",
        "§6§lGOOD CATCH! §r§bYou found a §r§fFish Bait§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§aGrand Experience Bottle§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§aBlessed Bait§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§fDark Bait§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§fLight Bait§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§aHot Bait§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§fSpooky Bait§r§b.",
    )

    // Winter Gift
    private val winterGiftPatterns = listOf(
        // winter gifts useless
        "§f§lCOMMON! §r§3.* XP §r§egift with §r.*§r§e!".toPattern(),
        "(§f§lCOMMON|§9§lRARE)! §r.* XP Boost .* Potion §r.*§r§e!".toPattern(),
        "(§f§lCOMMON|§9§lRARE)! §r§6.* coins §r§egift with §r.*§r§e!".toPattern(),

        // enchanted book
        "§9§lRARE! §r§9Scavenger IV §r§egift with §r.*§r§e!".toPattern(),
        "§9§lRARE! §r§9Looting IV §r§egift with §r.*§r§e!".toPattern(),
        "§9§lRARE! §r§9Luck VI §r§egift with §r.*§r§e!".toPattern(),

        // minion skin
        "§e§lSWEET! §r§f(Grinch|Santa|Gingerbread Man) Minion Skin §r§egift with §r.*§r§e!".toPattern(),

        // rune
        "§9§lRARE! §r§f◆ Ice Rune §r§egift with §r.*§r§e!".toPattern(),

        // furniture
        "§e§lSWEET! §r§fTall Holiday Tree §r§egift with §r.*§r§e!".toPattern(),
        "§e§lSWEET! §r§fNutcracker §r§egift with §r.*§r§e!".toPattern(),
        "§e§lSWEET! §r§fPresent Stack §r§egift with §r.*§r§e!".toPattern(),

        "§e§lSWEET! §r§9(Winter|Battle) Disc §r§egift with §r.*§r§e!".toPattern(),

        // winter gifts a bit useful
        "§e§lSWEET! §r§9Winter Sack §r§egift with §r.*§r§e!".toPattern(),
        "§e§lSWEET! §r§5Snow Suit .* §r§egift with §r.*§r§e!".toPattern(),

        // winter gifts not your gifts
        "§cThis gift is for §r.*§r§c, sorry!".toPattern(),
    )

    // Powder Mining
    private val powderMiningPatterns = listOf(
        "§cYou need a stronger tool to mine (Amethyst|Ruby|Jade|Amber|Sapphire|Topaz) Gemstone Block§r§c.".toPattern(),
        "§aYou received §r§f\\d* §r§f[❤❈☘⸕✎✧] §r§fRough (Ruby|Amethyst|Jade|Amber|Sapphire|Topaz) Gemstone§r§a\\.".toPattern(),
        "§aYou received §r§f\\d §r§a[❤❈☘⸕✎✧] §r§aFlawed (Ruby|Amethyst|Jade|Amber|Sapphire|Topaz) Gemstone§r§a\\.".toPattern(),

        // Jungle
        "§aYou received §r§f\\d* §r§aSludge Juice§r§a\\.".toPattern(),

        // Useful, maybe in another chat
        "§aYou received §r§b\\+\\d{1,3} §r§a(Mithril|Gemstone) Powder.".toPattern(),
        "§aYou received §r(§6|§b)\\+[1-2] (Diamond|Gold) Essence".toPattern(),
    )
    private val fireSalePatterns = listOf(
        "§c♨ §eFire Sales for .* §eare starting soon!".toPattern(),
        "§c\\s*♨ .* (?:Skin|Rune) §e(?:for a limited time )?\\(.* §eleft\\)(?:§c|!)".toPattern(),
        "§c♨ §eVisit the Community Shop in the next §c.* §eto grab yours! §a§l\\[WARP]".toPattern(),
        "§c♨ §eA Fire Sale for .* §eis starting soon!".toPattern(),
    )
    private val powderMiningMessages = listOf(
        "§aYou uncovered a treasure chest!",
        "§aYou received §r§f1 §r§aWishing Compass§r§a.",
        "§aYou received §r§f1 §r§9Ascension Rope§r§a.",
        // Jungle
        "§aYou received §r§f1 §r§aOil Barrel§r§a.",
        // Useful, maybe in another chat
        "§6You have successfully picked the lock on this chest!",
    )
    private val fireSaleMessages = listOf(
        "§6§k§lA§r §c§lFIRE SALE §r§6§k§lA",
        "§c♨ §eSelling multiple items for a limited time!",
    )

    private val patternsMap: Map<String, List<Pattern>> = mapOf(
        "lobby" to lobbyPatterns,
        "warping" to warpingPatterns,
        "guild_exp" to guildExpPatterns,
        "kill_combo" to killComboPatterns,
        "slayer" to slayerPatterns,
        "slayer_drop" to slayerDropPatterns,
        "useless_drop" to uselessDropPatterns,
        "useless_notification" to uselessNotificationPatterns,
        "money" to bazaarPatterns,
        "winter_island" to winterIslandPatterns,
        "annoying_spam" to annoyingSpamPatterns,
        "winter_gift" to winterGiftPatterns,
        "powder_mining" to powderMiningPatterns,
        "fire_sale" to fireSalePatterns,
    )

    private val messagesMap: Map<String, List<String>> = mapOf(
        "lobby" to lobbyMessages,
        "warping" to warpingMessages,
        "welcome" to welcomeMessages,
        "kill_combo" to killComboMessages,
        "bz_ah_minis" to miniBazaarAndAHMessages,
        "slayer" to slayerMessages,
        "useless_drop" to uselessDropMessages,
        "useless_notification" to uselessNotificationMessages,
        "party" to partyMessages,
        "money" to auctionHouseMessages,
        "useless_warning" to uselessWarningMessages,
        "annoying_spam" to annoyingSpamMessages,
        "powder_mining" to powderMiningMessages,
        "fire_sale" to fireSaleMessages,
    )
    private val messagesContainsMap: Map<String, List<String>> = mapOf(
        "lobby" to lobbyMessagesContains,
    )
    private val messagesStartsWithMap: Map<String, List<String>> = mapOf(
        "slayer" to slayerMessageStartWith,
        "profile_join" to profileJoinMessageStartsWith,
    )
    /// </editor-fold>

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        val blockReason = block(event.message)
        if (blockReason == "") return

        event.blockedReason = blockReason
    }

    /**
     * Checks if the message should be blocked
     * @param message The message to check
     * @return The reason why the message was blocked, empty if not blocked
     */
    private fun block(message: String): String = when {
        config.hypixelHub && message.isPresent("lobby") -> "lobby"
        config.empty && isEmpty(message) -> "empty"
        config.warping && message.isPresent("warping") -> "warping"
        config.welcome && message.isPresent("welcome") -> "welcome"
        config.guildExp && message.isPresent("guild_exp") -> "guild_exp"
        config.killCombo && message.isPresent("kill_combo") -> "kill_combo"
        config.profileJoin && message.isPresent("profile_join") -> "profile_join"

        config.others && isOthers(message) -> othersMsg

        config.winterGift && message.isPresent("winter_gift") -> "winter_gift"
        config.powderMining && message.isPresent("powder_mining") -> "powder_mining"
        config.fireSale && message.isPresent("fire_sale") -> "fire_sale"

        else -> ""
    }

    /**
     * Checks if the message is an empty message
     * @param message The message to check
     * @return True if the message is empty
     */
    private fun isEmpty(message: String) = message.removeColor().trimWhiteSpaceAndResets().isEmpty()

    private var othersMsg = ""

    /**
     * Checks if the message is an "other" message.
     * Will also set the variable othersMsg to the reason why the message was blocked,
     * so that it can be used in the block function.
     * @param message The message to check
     * @return True if the message is part of "other"
     * @see othersMsg
     * @see block
     */
    private fun isOthers(message: String): Boolean {
        othersMsg = when {
            message.isPresent("bz_ah_minis") -> "bz_ah_minis"
            message.isPresent("slayer") -> "slayer"
            message.isPresent("slayer_drop") -> "slayer_drop"
            message.isPresent("useless_drop") -> "useless_drop"
            message.isPresent("useless_notification") -> "useless_notification"
            message.isPresent("party") -> "party"
            message.isPresent("money") -> "money"
            message.isPresent("winter_island") -> "winter_island"
            message.isPresent("useless_warning") -> "useless_warning"
            message.isPresent("annoying_spam") -> "annoying_spam"
            else -> ""
        }
        return othersMsg != ""
    }

    /**
     * Checks if the message is present in the list of messages or patterns
     * Checks against four maps that compare in different ways.
     * @receiver message The message to check
     * @param key The key of the list to check
     * @return True if the message is present in any of the maps
     * @see messagesMap
     * @see patternsMap
     * @see messagesContainsMap
     * @see messagesStartsWithMap
     */
    private fun String.isPresent(key: String) = this in (messagesMap[key] ?: emptyList()) ||
        (patternsMap[key] ?: emptyList()).any { it.matches(this) } ||
        (messagesContainsMap[key] ?: emptyList()).any { this.contains(it) } ||
        (messagesStartsWithMap[key] ?: emptyList()).any { this.startsWith(it) }

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
