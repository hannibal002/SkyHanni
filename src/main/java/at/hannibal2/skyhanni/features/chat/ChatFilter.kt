package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTags
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.chat.PowderMiningChatFilter.genericMiningRewardMessage
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.gifting.GiftProfitTracker
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrEmpty
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.regex.Pattern
import kotlin.collections.listOf

@SkyHanniModule
@Suppress("LargeClass")
object ChatFilter {

    private val config get() = SkyHanniMod.feature.chat.filterType
    private val foragingConfig get() = config.foraging
    private val huntingConfig get() = config.hunting
    private val hypixelMessagesConfig get() = config.hypixelMessages
    private val transactionConfig get() = config.transaction
    private val slayerFilterConfig get() = config.slayers
    private val slayerFilterDropsConfig get() = config.slayers.slayerDrops
    private val gardenFilterConfig get() = config.garden
    private val dungeonFilterConfig get() = config.dungeon
    private val crystalNucleusConfig get() = config.crystalNucleus
    private val combatFilterConfig get() = config.combat
    private val warningsFilterConfig get() = config.warnings
    private val annoyingSpamFilterConfig get() = config.spam
    private val partyFilterConfig get() = config.party
    private val eventsFilterConfig get() = config.eventsFilter
    private val uselessDropsFilterConfig get() = config.uselessDrops
    private val uselessNotificationsFilterConfig get() = config.uselessNotifications


    private val chatFilterGroup = RepoPattern.group("chat-filter")
    private val huntingPatternGroup = chatFilterGroup.group("hunting")
    private val foragingPatternGroup = chatFilterGroup.group("foraging")
    private val miscPatternGroup = chatFilterGroup.group("hypixel-misc")

    // <editor-fold desc="Regex Patterns & Messages">

    // Annoying Spam Category
    private val baitMessages = listOf(
        "§6§lGOOD CATCH! §r§bYou found a §r§aBlessed Bait§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§aGrand Experience Bottle§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§aHot Bait§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§fDark Bait§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§fFish Bait§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§fLight Bait§r§b.",
        "§6§lGOOD CATCH! §r§bYou found a §r§fSpooky Bait§r§b.",
    )

    private val blessingMessages = listOf(
        "§aYour Blessing enchant got you double drops!",
    )

    private val blocksInTheWayMessages = listOf(
        "§cThere are blocks in the way!",
    )

    @Suppress("MaxLineLength")
    private val breakingPowerPattern = listOf(
        "§cYou need a tool with a §r§aBreaking Power §r§cof §r§6(\\d)§r§c to mine (.*)§r§c! Speak to §r§dFragilis §r§cby the entrance to the Crystal Hollows to learn more!".toPattern(),
    )

    private val cookieMessages = listOf(
        "§eObtain a §r§6Booster Cookie §r§efrom the community shop in the hub!",
    )

    private val sbeMessages = listOf(
        "§3[SBE] §a§cUnable to download bin data. This may result in certain features not working!",
    )

    private val sacrificePatterns = listOf(
        "§c§lBONUS LOOT! §r§eThey also received (.*) §r§efrom their sacrifice!".toPattern(),
        "§c§lSACRIFICE! (.*) §r§eturned (.*) §r§einto (.*) Dragon Essence§r§e!".toPattern(),
    )

    private val unknownCommandMessages = listOf(
        "Unknown command. Type \"/help\" for help. ('uhfdsolguhkjdjfhgkjhdfdlgkjhldkjhlkjhsldkjfhldshkjf')",
    )

    // Combat Category
    private val implosionMessages = listOf(
        "§7Your Implosion hit (.*) for §r§c(.*) §r§7damage.".toPattern(),
    )

    /**
     * REGEX-TEST: §a§l+5 Kill Combo §r§8+§r§b3% §r§b? Magic Find
     */
    private val killComboPatterns = listOf(
        "§.§l\\+(.*) Kill Combo (.*)".toPattern(),
        "§cYour Kill Combo has expired! You reached a (.*) Kill Combo!".toPattern(),
    )

    private val killComboMessages = listOf(
        "§6§l+50 Kill Combo",
    )

    private val moltenWaveMessages = listOf(
        "§7Your Molten Wave hit (.*) for §r§c(.*) §r§7damage.".toPattern(),
    )

    private val spiritSceptreMessages = listOf(
        "§7Your Spirit Sceptre hit (.*) for §r§c(.*) §r§7damage.".toPattern(),
    )

    // Dungeons Category
    private val fairyPatterns = listOf(
        "§d[\\w']+ the Fairy§r§f: Have a great life!".toPattern(),
        "§d[\\w']+ the Fairy§r§f: You killed me! I'll revive you so that my death is not in vain!".toPattern(),
        "§d[\\w']+ the Fairy§r§f: You killed me! Take this §r§6Revive Stone §r§fso that my death is not in vain!".toPattern(),
    )

    private val rareDropsMessages = listOf(
        "§6§lRARE REWARD! (.*) §r§efound a (.*) §r§ein their (.*) Chest§r§e!".toPattern(),
    )

    private val soloClassPatterns = listOf(
        "§6Your §r§a(Healer|Mage|Berserk|Archer|Tank) §r§6stats are doubled because you are the only player using this class!".toPattern(),
    )

    private val soloStatsPatterns = listOf(
        "§a\\[(Healer|Mage|Berserk|Archer|Tank)].*".toPattern(),
    )

    // Event Category
    // Chocolate Factory
    private val factoryUpgradePatterns = listOf(
        "§.* §r§7has been promoted to §r§7\\[.*§r§7] §r§.*§r§7!".toPattern(),
        "§7You upgraded to §r§d.*?§r§7!".toPattern(),
        "§7You will now produce §r§6.* Chocolate §r§7per click!".toPattern(),
        "§7Your §r§aRabbit Barn §r§7capacity has been increased to §r§a.* Rabbits§r§7!".toPattern(),
    )
    // Winter Island
    private val winterGiftPatterns = buildList {
        GiftProfitTracker.run {
            listOf(
                xpGainedPattern,
                coinsGainedPattern,
                northStarsPattern,
                boostPotionPattern,
                enchantmentBookPattern,
                genericRewardPattern,
            ).forEach { add(it) }
        }
        addAll(GiftProfitTracker.spamPatterns)
    }
    // Snow Cannon
    private val winterIslandPatterns = listOf(
        "§r§f☃ §r§7§r(.*) §r§7mounted a §r§fSnow Cannon§r§7!".toPattern(),
    )

    // Foraging Category
    /**
     ** REGEX-TEST: §cYou cannot damage a tree while it is regenerating!
     ** REGEX-TEST: §c§oThe toughness of this tree is way too high!
     */
    private val unmineableTreePatterns by foragingPatternGroup.list(
        "unmineable-tree",
        "(?:§.)*The toughness of this tree is way too high!",
        "(?:§.)*You cannot damage a tree while it is regenerating!",
    )

    // Garden Category
    /**
     * REGEX-TEST: §e[NPC] Jacob§f: §rYour §9Anita's Talisman §fis giving you §6+25☘ Carrot Fortune §fduring the contest!
     */
    private val anitaFortunePattern by RepoPattern.pattern(
        "chat.jacobevent.accessory",
        "§e\\[NPC] Jacob§f: §rYour §9Anita's \\w+ §fis giving you §6\\+\\d{1,2}☘ .+ Fortune §fduring the contest!",
    )

    private val jacobsContestStart = listOf(
        "§e[NPC] Jacob§f: §rMy contest has started!"
    )

    // Hunting Category
    /**
     ** REGEX-TEST: §7§oMochibear ate too much and passed out! You caught it!
     ** REGEX-TEST: §7§oYou caught yourself an invisibug! The shard was sent to your Hunting Box!
     ** REGEX-TEST: §7§oThe Frog is exhausted...
     */
    private val redundantShardsPatterns by huntingPatternGroup.list(
        "redundant-comments",
        "(?:§.)*Mochibear ate too much and passed out! You caught it!",
        "(?:§.)*The Frog is exhausted\\.\\.\\.",
        "(?:§.)*You caught yourself an invisibug! The shard was sent to your Hunting Box!",
    )

    /**
     * REGEX-TEST: §e[NPC] §bSwoop§f: §rWow! I forgot to tell you, monsters around here can only take damage from Axes!
     */
    private val swoopAxePattern by huntingPatternGroup.pattern(
        "swoop-axe-message",
        "§e\\[NPC] §bSwoop§f: §rWow! I forgot to tell you, monsters around here can only take damage from Axes!"
    )

    private val charmMessageStartsWith = listOf(
        "§5§lCHARM§7 You charmed a",
        "§d§lCHARM§7 You charmed a",
    )

    // Hypixel Messages Category
    /**
     * REGEX-TEST: §e§ka§a>>   §aAchievement Unlocked: §6§r§6Agile§r§a   <<§e§ka
     */
    private val achievementGetPatterns = listOf(
        "§e§k.§a>> {3}§aAchievement Unlocked: .* {3}<<§e§k.".toPattern(),
    )

    private val eventMessage = listOf(
        "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
    )

    private val eventPatterns = listOf(
        "(?:§f)? +§r§.§k#§r§. LEVEL UP! §r§.§k#".toPattern(),
        "(?:§f)? +§r§7You are now §r§.Event Level §r§.*§r§7!".toPattern(),
        "(?:§f)? +§r§7You earned §r§.* Event Silver§r§7!".toPattern(),
    )

    private val fireSaleMessages = listOf(
        "§6§k§lA§r §c§lFIRE SALE §r§6§k§lA",
        "§c♨ §eSelling multiple items for a limited time!",
    )

    private val fireSalePattern by RepoPattern.pattern(
        "chat.firesale",
        "§6§k§lA§r §c§lFIRE SALE §r§6§k§lA(?:\\n|.)*",
    )

    private val fireSalePatterns = listOf(
        "§c {3}♨ §eAnd \\d+ more!".toPattern(),
        "§c♨ §eA Fire Sale for .* §eis starting soon!".toPattern(),
        "§c♨ §eFire Sales for .* §eare starting soon!".toPattern(),
        "§c♨ §eVisit the Community Shop in the next §c.* §eto grab yours! §a§l\\[WARP]".toPattern(),
        "§c♨ §r§eFire Sales? for .* §r§eended!".toPattern(),
        "§c\\s*♨ .* (?:Skin|Rune|Dye) §e(?:for a limited time )?\\(.* §eleft\\)(?:§c|!)".toPattern(),
    )

    // Friend Join/leave
    /**
     * REGEX-TEST: §aFriend > §r§bUsername §r§ejoined.
     * REGEX-TEST: §aFriend > §r§7Username §r§ejoined.
     * REGEX-TEST: §aFriend > §r§aUsername §r§eleft.
     * REGEX-TEST: §aFriend > §r§6Username §r§eleft.
     */
    private val friendJoinLeftPatterns = listOf(
        "§aFriend > §r§[0-9a-f].*?§r§e(joined|left)\\.".toPattern()
    )

    // Guild EXP
    /**
     * REGEX-TEST: §aYou earned §r§22 GEXP §r§afrom playing SkyBlock!
     * REGEX-TEST: §aYou earned §r§22 GEXP §r§a+ §r§c210 Event EXP §r§afrom playing SkyBlock!
     */
    private val guildExpPatterns = listOf(
        "§aYou earned §r§2.* GEXP (§r§a\\+ §r§.* Event EXP )?§r§afrom playing SkyBlock!".toPattern(),
    )

    private val legacyItems = listOf(
        (
            "§cYou currently have one or more Legacy Items in your inventory or sacks that are no longer" +
                " used throughout the game! Exchange them in the Legacy Trades menu, accessed through /legacytrades!"
            ).toPattern(),
    )


    private val lobbyMessages = listOf(
        // prototype
        "  §r§f§l➤ §r§6You have reached your Hype limit! Add Hype to Prototype Lobby minigames" +
            " by right-clicking with the Hype Diamond!",
    )

    private val lobbyMessagesContains = listOf(
        // prototype
        "§r§6§lWelcome to the Prototype Lobby§r",
        // hypixel tournament notifications
        "§r§e§6§lHYPIXEL BED WARS DOUBLES§e tournament is live!",
        "§r§e§6§lHYPIXEL§e is hosting a §b§lBED WARS DOUBLES§e tournament!",
        "§r§e§6§lHYPIXEL§e is hosting a §b§lTNT RUN§e tournament!",
        // other
        "§aYou are still radiating with §bGenerosity§r§a!",
    )

    @Suppress("MaxLineLength")
    private val lobbyPatterns = listOf(
        // player join
        "(?: §b>§c>§a>§r §r)?.* §6(?:joined|(?:spooked|slid) into) the lobby!(?:§r §a<§c<§b<)?".toPattern(),
        // Hypixel SMP
        "§2[\\s]*?§aYou can now create your own Hypixel SMP server![\\s]*?".toPattern(),
        // Snow Particles in the Lobby
        "[\\s]*?.*§bFor the best experience, click the text below to enable Snow[\\s]§.*§bParticles in this lobby![\\s]*?.*§3§lClick to enable Snow Particles[\\s]*?".toPattern(),
        // mystery box
        "§b✦ §r.* §r§7found (a|an) §r.* §r§7in a §r§a(Holiday )?Mystery Box§r§7!".toPattern(),
        "§b✦ §r.* §r§7found a §r§e.* §r§bMystery Box§r§7!".toPattern(),
        // mystery dust
        "§b✦ §r§7You earned §r§b\\d+ §r§7Mystery Dust!".toPattern(),
        // pet consumables
        "§b§b✦ §r§7You earned §a\\d+ §7Pet Consumables items!".toPattern(),
    )

    private val lotteryMessages = listOf(
        "§8§oYou can disable this messaging by toggling Lottery in your /hotf!",
        "§bNew day! §r§eYour §r§2Lottery §r§ebuff changed!",
    )

    /**
     * REGEX-TEST: §aStarted parkour cocoa!
     * REGEX-TEST: §aFinished parkour cocoa in 12:34.567!
     * REGEX-TEST: §aReached checkpoint #4 for parkour cocoa!
     * REGEX-TEST: §4Wrong checkpoint for parkour cocoa!
     * REGEX-TEST: §4You haven't reached all checkpoints for parkour cocoa!
     */
    private val parkourPatterns = listOf(
        "§4Wrong checkpoint for parkour (.*)!".toPattern(),
        "§4You haven't reached all checkpoints for parkour (.*)!".toPattern(),
        "§aFinished parkour (.*) in (.*)!".toPattern(),
        "§aReached checkpoint #(.*) for parkour (.*)!".toPattern(),
        "§aStarted parkour (.*)!".toPattern(),
    )

    /**
     * REGEX-TEST: §4Cancelled parkour! You cannot fly.
     * REGEX-TEST: §4Cancelled parkour! You cannot use item abilities.
     * REGEX-TEST: §4Cancelled parkour!
     */
    private val parkourCancelMessages = listOf(
        "§4Cancelled parkour!",
        "§4Cancelled parkour! You cannot fly.",
        "§4Cancelled parkour! You cannot use item abilities.",
    )

    private val profileJoinMessageStartsWith = listOf(
        "§8Profile ID: ",
        "§aYou are playing on profile: §e",
    )

    /**
     ** REGEX-TEST: §eYou haven't claimed your §r§6Summer Rewards §r§eyet!
     ** REGEX-TEST: §eTalk to the §r§bSummer Sloth §r§ein the §r§aHub§r§e!
     ** REGEX-TEST: §eTalk to the §r§bRandom NPC §r§ein the §r§aForbidden Zone§r§e!
     */
    private val rewardBundlePatterns by miscPatternGroup.list(
        "seasonal-bundles",
        "(?:§.)*Talk to the (?:§.)*.+(?:§.)*in the (?:§.)*.+(?:§.)*!",
        "(?:§.)*You haven't claimed your (?:§.)*\\w+ Rewards (?:§.)*yet!",
    )

    private val skymallMessages = listOf(
        "§8§oYou can disable this messaging by toggling Sky Mall in your /hotm!",
        "§bNew day! §r§eYour §r§2Sky Mall §r§ebuff changed!",
    )

    private val teleportPadMessages = listOf(
        "§4This Teleport Pad does not have a destination set!",
    )

    /**
     ** REGEX-TEST: §r§aWarped from the tpPadOne §r§ato the tpPadTwo§r§a!
     */
    private val teleportPadPatterns = listOf(
        "§aWarped from the (.*) §r§ato the (.*)§r§a!".toPattern(),
    )

    private val tipPlayerPatterns = listOf(
        "(?:§a)?§aYou tipped \\d+ players? in \\d+(?: different)? games?!".toPattern(),
    )

    private val welcomeMessages = listOf(
        "§eWelcome to §r§aHypixel SkyBlock§r§e!",
    )

    private val warpingMessages = listOf(
        "§7Finding player...",
        "§7Sending a visit request...",
        "§7Warping using transfer token...",
        "§7Warping you to your SkyBlock island...",
        "§7Warping...",
    )

    private val warpingPatterns = listOf(
        "§7Request join for Dungeon Hub #(.*)\\.\\.\\.".toPattern(),
        "§7Request join for Hub (.*)\\.\\.\\.".toPattern(),
        "§7Sending to server (.*)\\.\\.\\.".toPattern(),
        "§dWarped to (.*)§r§d!".toPattern(),
    )

    // Party Category
    private val partyLineMessages = listOf(
        "§9§m-----------------------------------------------------",
        "§9§m§9§m-----------------------------------------------------"
    )

    // Powder Mining Category
    private val powderMiningMessages = listOf(
        "§aYou received §r§f1 §r§9Ascension Rope§r§a.",
        "§aYou received §r§f1 §r§aOil Barrel§r§a.",
        "§aYou received §r§f1 §r§aWishing Compass§r§a.",
        "§aYou uncovered a treasure chest!",
        "§6You have successfully picked the lock on this chest!",
    )

    // Slayer Category
    private val slayerCompletePattern = listOf(
        // end
        " {2}§r§a§lSLAYER QUEST COMPLETE!".toPattern(),
    )

    private val slayerKilledMessage = listOf(
        "  §r§6§lNICE! SLAYER BOSS SLAIN!",
        "§eYou received kill credit for assisting on a slayer miniboss!",
    )

    private val slayerLevelPattern = listOf(
        " {3}§r§e(.*)Slayer LVL 9 §r§5- §r§a§lLVL MAXED OUT!".toPattern(),
    )

    private val slayerMaddoxPattern = listOf(
        " {3}§r§5§l» §r§7Talk to Maddox to claim your (.*) Slayer XP!".toPattern(),
    )

    private val slayerRingMessage = listOf(
        "§e✆ RING... ",
    )

    private val slayerStartPatterns = listOf(
        // start
        " {2}§r§5§lSLAYER QUEST STARTED!".toPattern(),
        " {3}§5§l» §7Slay §c(.*) Combat XP §7worth of (.*)§7.".toPattern(),
    )

    // Transactions Category
    private val auctionHouseClaim = listOf(
        "§b-----------------------------------------------------",
        "§eVisit the Auction House to collect your item!",
    )

    private val allowanceMessages = listOf(
        "§6§lALLOWANCE! §r§eYou earned §r§6(.*) coins§r§e!"
    )

    private val bankDepositWithdraw = listOf(
        "§8Depositing coins...",
        "§8Withdrawing coins...",
    )

    private val bazaarCancelMessage = listOf(
        "§6[Bazaar] §r§7Cancelling order...",
    )

    private val bazaarClaimMessage = listOf(
        "§6[Bazaar] §r§7Claiming order...",
    )

    private val bazaarEscrowMessage = listOf(
        "§6[Bazaar] §r§7Putting goods in escrow...",
        "§7Putting coins in escrow...",
        "§7Putting item in escrow...",
    )

    private val bazaarInstantMessages = listOf(
        "§6[Bazaar] §r§7Executing instant buy...",
        "§6[Bazaar] §r§7Executing instant sell...",
    )

    private val bazaarOrderMessages = listOf(
        "§6[Bazaar] §r§7Submitting buy order...",
        "§6[Bazaar] §r§7Submitting sell offer...",
    )

    private val uselessAHMessages = listOf(
        "§7Claiming BIN auction...",
        "§7Processing bid...",
        "§7Processing purchase...",
        "§7Setting up the auction...",
    )

    // Useless Drop Category

    private val uselessDropCombatMessages = listOf(
        "§6§lRARE DROP! §r§aEnchanted Ender Pearl",
        "§6§lRARE DROP! §r§aEnchanted End Stone",
        "§6§lRARE DROP! §r§5Crystal Fragment",
    )

    private val uselessDropDungeonsPatterns = listOf(
        "§6§lRARE DROP! §r§5Earth Shard (.*)".toPattern(),
        "§6§lRARE DROP! §r§5Zombie Lord Chestplate (.*)".toPattern(),
        "§6§lRARE DROP! §r§9Machine Gun Bow (.*)".toPattern(),
    )

    private val uselessDropCombatPatterns = listOf(
        "§6§lRARE DROP! §r§aEnchanted Ender Pearl (.*)".toPattern(),
        "§6§lRARE DROP! §r§fCarrot (.*)".toPattern(),
        "§6§lRARE DROP! §r§fPotato (.*)".toPattern(),
    )

    // Useless Notification Category
    private val brokeBankMessages = listOf(
        "§aSince you've been away you earned §r§60 coins §r§aas interest in your personal bank account!",
        "§aYou have just received §r§60 coins §r§aas interest in your co-op bank account!",
        "§aYou have just received §r§60 coins §r§aas interest in your personal bank account!",
    )

    private val powerOrbNotificationPattern = listOf(
        "§eYour previous §r§[0-9a-f](Plasmaflux Power Orb|Overflux Power Orb|Manaflux Power Orb) §r§ewas removed!".toPattern()
    )

    private val miningSpeedBoostNotificationMessages = listOf(
        "§aYou used your §r§6Mining Speed Boost §r§aPickaxe Ability!",
        "§a§r§6Mining Speed Boost §r§ais now available!",
        "§cYour Mining Speed Boost has expired!",
    )

    // Useless Warnings Category
    private val abilityCooldownPattern = listOf(
        "§cThis ability is on cooldown for (.*)s.".toPattern()
    )

    private val commandsFastWarning = listOf(
        "§cYou are sending commands too fast! Please slow down.", // TODO prevent in the future
    )

    private val combatWarning = listOf(
        "§cYou can't use this while in combat!",
        "§cYou cannot open the SkyBlock menu while in combat!",
        "§cYou can't use the wardrobe in combat!",
    )

    private val modifyWarning = listOf(
        "§cYou can not modify your equipped armor set!",
    )

    private val refreshingWarning = listOf(
        "§cPlease wait a few seconds between refreshing!",
    )

    private val salvageableWarning = listOf(
        "§cThis item is not salvageable!", // TODO prevent in the future
    )

    private val salvageWarning = listOf(
        "§cPlace a Dungeon weapon or armor piece above the anvil to salvage it!",
    )

    private val slowDownWarning = listOf(
        "§cWhoa! Slow down there!",
    )

    private val waitWarning = listOf(
        "§cWait a moment before confirming!",
    )

    private val messagesContainsMap: Map<String, List<String>> = mapOf(
        "lobby" to lobbyMessagesContains,
    )

    private val messagesMap: Map<String, List<String>> = mapOf(
        "ah_claim" to auctionHouseClaim,
        "allowance" to allowanceMessages,
        "bank_transaction" to bankDepositWithdraw,
        "bazaar_cancel" to bazaarCancelMessage,
        "bazaar_claim" to bazaarClaimMessage,
        "bazaar_escrow" to bazaarEscrowMessage,
        "bazaar_instant" to bazaarInstantMessages,
        "bazaar_order" to bazaarOrderMessages,
        "blessing" to blessingMessages,
        "block_way" to blocksInTheWayMessages,
        "commands_fast" to commandsFastWarning,
        "combat_warning" to combatWarning,
        "cookie" to cookieMessages,
        "drops_combat" to uselessDropCombatMessages,
        "event" to eventMessage,
        "fire_sale" to fireSaleMessages,
        "bait" to baitMessages,
        "jacob_start" to jacobsContestStart,
        "kill_combo" to killComboMessages,
        "lobby" to lobbyMessages,
        "lottery" to lotteryMessages,
        "mining_boost" to miningSpeedBoostNotificationMessages,
        "modify_warning" to modifyWarning,
        "parkour" to parkourCancelMessages,
        "party_line" to partyLineMessages,
        "powder_mining" to powderMiningMessages,
        "refresh_warning" to refreshingWarning,
        "salvageable_warning" to salvageableWarning,
        "salvage_warning" to salvageWarning,
        "sbe" to sbeMessages,
        "skymall" to skymallMessages,
        "slayer_killed" to slayerKilledMessage,
        "slayer_ring" to slayerRingMessage,
        "slow_warning" to slowDownWarning,
        "teleport_pads" to teleportPadMessages,
        "unknown_command" to unknownCommandMessages,
        "useless_ah" to uselessAHMessages,
        "broke" to brokeBankMessages,
        "wait_warning" to waitWarning,
        "warping" to warpingMessages,
        "welcome" to welcomeMessages,
    )

    private val messagesStartsWithMap: Map<String, List<String>> = mapOf(
        "profile_join" to profileJoinMessageStartsWith,
        "slayer_ring" to slayerRingMessage,
        "charm" to charmMessageStartsWith,
    )

    private val patternsMap: Map<String, List<Pattern>> = mapOf(
        "ability_cooldown" to abilityCooldownPattern,
        "achievement_get" to achievementGetPatterns,
        "breaking_power" to breakingPowerPattern,
        "drops_combat" to uselessDropCombatPatterns,
        "drops_dungeons" to uselessDropDungeonsPatterns,
        "event" to eventPatterns,
        "fairy" to fairyPatterns,
        "factory_upgrade" to factoryUpgradePatterns,
        "fire_sale" to fireSalePatterns,
        "friendJoinLeft" to friendJoinLeftPatterns,
        "guild_exp" to guildExpPatterns,
        "implosion" to implosionMessages,
        "kill_combo" to killComboPatterns,
        "legacy_items" to legacyItems,
        "lobby" to lobbyPatterns,
        "molten_wave" to moltenWaveMessages,
        "parkour" to parkourPatterns,
        "power_orb" to powerOrbNotificationPattern,
        "rare_drops" to rareDropsMessages,
        "sacrifice" to sacrificePatterns,
        "slayer_complete" to slayerCompletePattern,
        "slayer_level" to slayerLevelPattern,
        "slayer_maddox" to slayerMaddoxPattern,
        "slayer_start" to slayerStartPatterns,
        "solo_class" to soloClassPatterns,
        "solo_stats" to soloStatsPatterns,
        "spirit_sceptre" to spiritSceptreMessages,
        "teleport_pads" to teleportPadPatterns,
        "tip" to tipPlayerPatterns,
        "warping" to warpingPatterns,
        "winter_gift" to winterGiftPatterns,
        "winter_island" to winterIslandPatterns,
    )

    private val repoPatternsMap: Map<String, List<Pattern>> = mapOf(
        "redundant_hunting" to redundantShardsPatterns,
        "reward_bundles" to rewardBundlePatterns,
        "unmineable_tree" to unmineableTreePatterns,
        "swoop_axe" to listOf(swoopAxePattern),
    )

    // </editor-fold>

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent) {
        var blockReason = block(event.message)
        if (blockReason == null && config.powderMining.enabled) blockReason = powderMiningBlock(event)
        if (blockReason == null && config.crystalNucleus.enabled) blockReason = crystalNucleusBlock(event)

        event.blockedReason = blockReason ?: return
    }

    /**
     * Checks if the message should be blocked
     * @param message The message to check
     * @return The reason why the message was blocked, empty if not blocked
     */
    @Suppress("CyclomaticComplexMethod", "MaxLineLength")
    private fun block(message: String): String? = when {
        // AnnoyingSpam
        annoyingSpamFilterConfig.bait && message.isPresent("bait") -> "bait"
        annoyingSpamFilterConfig.blessing && message.isPresent("blessing") -> "blessing"
        annoyingSpamFilterConfig.blockWay && message.isPresent("block_way") -> "block_way"
        annoyingSpamFilterConfig.breakingPower && message.isPresent("breaking_power") -> "breaking_power"
        annoyingSpamFilterConfig.cookie && message.isPresent("cookie") -> "cookie"
        annoyingSpamFilterConfig.sacrifice && message.isPresent("sacrifice") -> "sacrifice"
        annoyingSpamFilterConfig.sbe && message.isPresent("sbe") -> "sbe"
        // Combat
        combatFilterConfig.killCombo && message.isPresent("kill_combo") -> "kill_combo"
        combatFilterConfig.implosion && message.isPresent("implosion") -> "implosion"
        combatFilterConfig.spiritSceptre && message.isPresent("spirit_sceptre") -> "spirit_sceptre"
        combatFilterConfig.moltenWave && message.isPresent("molten_wave") -> "molten_wave"
        // Crystal Nucleus
        crystalNucleusConfig.hideSkyMall && !IslandTypeTags.MINING.inAny() && message.isPresent("skymall") -> "skymall"
        // Dungeons
        dungeonFilterConfig.rareDrops && message.isPresent("rare_drops") -> "rare_drops"
        dungeonFilterConfig.soloClass && DungeonApi.inDungeon() && message.isPresent("solo_class") -> "solo_class"
        dungeonFilterConfig.soloStats && DungeonApi.inDungeon() && message.isPresent("solo_stats") -> "solo_stats"
        dungeonFilterConfig.fairy && DungeonApi.inDungeon() && message.isPresent("fairy") -> "fairy"
        // Events
        eventsFilterConfig.cannon && message.isPresent("winter_island") -> "winter_island"
        eventsFilterConfig.winterGift && message.isPresent("winter_gift") -> "winter_gift"
        eventsFilterConfig.factoryUpgrade && message.isPresent("factory_upgrade") -> "factory_upgrade"
        // Foraging
        foragingConfig.unmineable && IslandTypeTags.FORAGING_CUSTOM_TREES.inAny() && message.isPresent("unmineable_tree") -> "unmineable_tree"
        foragingConfig.hideLottery && !IslandTypeTags.FORAGING.inAny() && message.isPresent("lottery") -> "lottery"
        // Garden
        gardenFilterConfig.hideJacob && !GardenApi.inGarden() && anitaFortunePattern.matches(message) -> "jacob_event"
        gardenFilterConfig.gardenNoPest && GardenApi.inGarden() && PestApi.noPestsChatPattern.matches(message) -> "garden_pest"
        gardenFilterConfig.jacobStart && message.isPresent("jacob_start") -> "jacob_start"
        // Hunting
        huntingConfig.redundantComments && IslandType.GALATEA.isCurrent() && message.isPresent("redundant_hunting") -> "redundant_hunting"
        huntingConfig.swoopAxeMessage && message.isPresent("swoop_axe") -> "swoop_axe"
        huntingConfig.charm && message.isPresent("charm") -> "charm"
        // HypixelMessages
        hypixelMessagesConfig.hypixelHub && message.isPresent("lobby") -> "lobby"
        hypixelMessagesConfig.warping && message.isPresent("warping") -> "warping"
        hypixelMessagesConfig.empty && StringUtils.isEmpty(message) -> "empty"
        hypixelMessagesConfig.welcome && message.isPresent("welcome") -> "welcome"
        hypixelMessagesConfig.guildExp && message.isPresent("guild_exp") -> "guild_exp"
        hypixelMessagesConfig.friendJoinLeft && message.isPresent("friendJoinLeft") -> "friendJoinLeft"
        hypixelMessagesConfig.profileJoin && message.isPresent("profile_join") -> "profile_join"
        hypixelMessagesConfig.parkour && message.isPresent("parkour") -> "parkour"
        hypixelMessagesConfig.teleportPads && message.isPresent("teleport_pads") -> "teleport_pads"
        hypixelMessagesConfig.tip && message.isPresent("tip") -> "tip"
        hypixelMessagesConfig.hideAlphaAchievements && HypixelData.hypixelAlpha && message.isPresent("achievement_get") -> "achievement_get"
        hypixelMessagesConfig.fireSale && (fireSalePattern.matches(message) || message.isPresent("fire_sale")) -> "fire_sale"
        hypixelMessagesConfig.rewardBundles && message.isPresent("reward_bundles") -> "reward_bundles"
        // TODO need proper solution to hide empty messages in event text
        hypixelMessagesConfig.eventLevelUp && (message.isPresent("event")) -> "event"
        hypixelMessagesConfig.legacyItemsWarning && message.isPresent("legacy_items") -> "legacy_items"
        // Party
        partyFilterConfig.partyLine && message.isPresent("party_line") -> "party_line"
        // Slayers
        slayerFilterConfig.slayerStart && message.isPresent("slayer_start") -> "slayer_start"
        slayerFilterConfig.slayerKilled && message.isPresent("slayer_killed") -> "slayer_killed"
        slayerFilterConfig.slayerComplete && message.isPresent("slayer_complete") -> "slayer_complete"
        slayerFilterConfig.slayerMaddox && message.isPresent("slayer_maddox") -> "slayer_maddox"
        slayerFilterConfig.slayerLevel && message.isPresent("slayer_level") -> "slayer_level"
        slayerFilterConfig.slayerRing && message.isPresent("slayer_ring") -> "slayer_ring"
        slayerFilterDropsConfig.enabled && SlayerFilterDrops.block(message)?.let {
            if (it != "no_filter") it else null
        } != null -> SlayerFilterDrops.block(message)
        // Transactions
        transactionConfig.allowance && message.isPresent("allowance") -> "allowance"
        transactionConfig.bazaarEscrow && message.isPresent("bazaar_escrow") -> "bazaar_escrow"
        transactionConfig.bazaarOrder && message.isPresent("bazaar_order") -> "bazaar_order"
        transactionConfig.bazaarInstant && message.isPresent("bazaar_instant") -> "bazaar_instant"
        transactionConfig.bazaarCancel && message.isPresent("bazaar_cancel") -> "bazaar_cancel"
        transactionConfig.bazaarClaim && message.isPresent("bazaar_claim") -> "bazaar_claim"
        transactionConfig.bankDepositWithdraw && message.isPresent("bank_transaction") -> "bank_transaction"
        transactionConfig.auctionHouseClaim && message.isPresent("ah_claim") -> "ah_claim"
        transactionConfig.uselessAH && message.isPresent("useless_ah") -> "useless_ah"
        // Useless Drops
        uselessDropsFilterConfig.combatDrops && message.isPresent("drops_combat") -> "drops_combat"
        uselessDropsFilterConfig.dungeonDrops && message.isPresent("drops_dungeon") -> "drops_dungeon"
        // Useless Notifications
        uselessNotificationsFilterConfig.broke && message.isPresent("broke") -> "broke"
        uselessNotificationsFilterConfig.miningSpeed && message.isPresent("mining_speed") -> "mining_speed"
        uselessNotificationsFilterConfig.powerOrb && message.isPresent("power_orb") -> "power_orb"
        // Useless Warnings
        warningsFilterConfig.abilityCooldown && message.isPresent("ability_cooldown") -> "ability_cooldown"
        warningsFilterConfig.fastCommand && message.isPresent("commands_fast") -> "commands_fast"
        warningsFilterConfig.combatWarning && message.isPresent("combat_warning") -> "combat_warning"
        warningsFilterConfig.modifyWarning && message.isPresent("modify_warning") -> "modify_warning"
        warningsFilterConfig.refreshWarning && message.isPresent("refresh_warning") -> "refresh_warning"
        warningsFilterConfig.salvageableWarning && message.isPresent("salvageable_warning") -> "salvageable_warning"
        warningsFilterConfig.salvageWarning && message.isPresent("salvage_warning") -> "salvage_warning"
        warningsFilterConfig.slowWarning && message.isPresent("slow_warning") -> "slow_warning"
        warningsFilterConfig.waitWarning && message.isPresent("wait_warning") -> "wait_warning"

        else -> null
    }

    /**
     * Checks if the message is a blocked powder mining message, as defined in PowderMiningChatFilter.
     * Will modify un-filtered Mining rewards, or return a resultant blocking code
     * @param event The event to check
     * @return Block reason if applicable
     * @see block
     */
    private fun powderMiningBlock(event: SkyHanniChatEvent): String? {
        val powderMiningMatchResult = PowderMiningChatFilter.block(event.message)
        if (powderMiningMatchResult == "no_filter") {
            genericMiningRewardMessage.matchMatcher(event.message) {
                val reward = groupOrEmpty("reward")
                val amountFormat = groupOrNull("amount")?.let {
                    "§a+ §b$it§r"
                } ?: "§a+§r"
                event.chatComponent = "$amountFormat $reward".asComponent()
            }
            return null
        }
        return powderMiningMatchResult
    }

    /**
     * Checks if the message is a blocked Crystal Nucleus Run message, as defined in CrystalNucleusChatFilter.
     * Will conditionally modify/compact messages in some cases, or return a blocking code
     * @param event The event to check
     * @return Block reason if applicable
     * @see block
     */
    private fun crystalNucleusBlock(event: SkyHanniChatEvent): String? {
        val (blockCode, newMessage) = CrystalNucleusChatFilter.block(event.message)?.getPair() ?: Pair(null, null)
        newMessage?.let { event.chatComponent = it.asComponent() }
        blockCode?.let { return it }
        return null
    }

    /**
     * Checks if the message is present in the list of messages or patterns
     * Checks against four maps that compare in different ways.
     * @receiver message The message to check
     * @param key The key of the list to check
     * @return True if the message is present in any of the maps
     * @see messagesMap
     * @see patternsMap
     * @see repoPatternsMap
     * @see messagesContainsMap
     * @see messagesStartsWithMap
     */
    private fun String.isPresent(key: String) = this in (messagesMap[key].orEmpty()) ||
        (patternsMap[key].orEmpty()).any { it.matches(this) } ||
        (repoPatternsMap[key].orEmpty()).any { it.matches(this) } ||
        (messagesContainsMap[key].orEmpty()).any { this.contains(it) } ||
        (messagesStartsWithMap[key].orEmpty()).any { this.startsWith(it) }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(98, "chat.filterType.hypixelHub", "chat.filterType.hypixelMessages.hypixelHub")
        event.move(98, "chat.filterType.welcome", "chat.filterType.hypixelMessages.welcome")
        event.move(98, "chat.filterType.empty", "chat.filterType.hypixelMessages.empty")
        event.move(98, "chat.filterType.warping", "chat.filterType.hypixelMessages.warping")
        event.move(98, "chat.filterType.guildExp", "chat.filterType.hypixelMessages.guildExp")
        event.move(98, "chat.filterType.friendJoinLeft", "chat.filterType.hypixelMessages.friendJoinLeft")
        event.move(98, "chat.filterType.profileJoin", "chat.filterType.hypixelMessages.profileJoin")
        event.move(98, "chat.filterType.fireSale", "chat.filterType.hypixelMessages.fireSale")
        event.move(98, "chat.filterType.rewardBundles", "chat.filterType.hypixelMessages.rewardBundles")
        event.move(98, "chat.filterType.eventLevelUp", "chat.filterType.hypixelMessages.eventLevelUp")
        event.move(98, "chat.filterType.hideAlphaAchievements", "chat.filterType.hypixelMessages.hideAlphaAchievements")
        event.move(98, "chat.filterType.parkour", "chat.filterType.hypixelMessages.parkour")
        event.move(98, "chat.filterType.teleportPads", "chat.filterType.hypixelMessages.teleportPads")
        event.move(98, "chat.filterType.legacyItemsWarning", "chat.filterType.hypixelMessages.legacyItemsWarning")
        event.move(98, "chat.hideLottery", "chat.filterType.foraging.hideLottery")
        event.move(98, "chat.dungeonFilteredMessageTypes", "chat.filterType.dungeon.dungeonFilteredMessageTypes")
        event.move(98, "chat.dungeonBossMessages", "chat.filterType.dungeon.dungeonBossMessages")
        event.move(98, "chat.hideLottery", "chat.filterType.foraging.legacyItemsWarning")
        event.move(98, "chat.winterGift", "chat.filterType.winterGift")
        event.move(98, "chat.powderMining", "chat.filterType.powderMining")
        event.move(98, "chat.killCombo", "chat.filterType.killCombo")
        event.move(98, "chat.filterType.powderMining", "chat.filterType.powderMiningFilter.enabled")
        event.transform(53, "chat.filterType.powderMiningFilter.gemstoneFilterConfig") { element ->
            element.asJsonObject.apply {
                entrySet().forEach { (key, value) ->
                    if (value.asString == "FINE_ONLY") addProperty(key, "FINE_UP")
                }
            }
        }
        event.move(61, "chat.filterType.powderMiningFilter", "chat.filterType.powderMining")
        event.move(61, "chat.filterType.gemstoneFilterConfig", "chat.filterType.powderMining.gemstone")
    }
}
