package at.hannibal2.skyhanni.features.chat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.IslandTypeTag
import at.hannibal2.skyhanni.data.model.SkyblockStat
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

@SkyHanniModule
object ChatFilter {

    private val generalConfig get() = SkyHanniMod.feature.chat
    private val config get() = SkyHanniMod.feature.chat.filterType
    private val dungeonConfig get() = SkyHanniMod.feature.dungeon.messageFilter
    private val foragingConfig get() = config.foraging
    private val huntingConfig get() = config.hunting

    private val chatFilterGroup = RepoPattern.group("chat-filter")
    private val huntingPatternGroup = chatFilterGroup.group("hunting")
    private val foragingPatternGroup = chatFilterGroup.group("foraging")
    private val miscPatternGroup = chatFilterGroup.group("hypixel-misc")
    private val eventPatternGroup = chatFilterGroup.group("event")

    // <editor-fold desc="Regex Patterns & Messages">
    // Lobby Messages
    @Suppress("MaxLineLength")
    private val lobbyPatterns by miscPatternGroup.list(
        // player join
        "(?: >>> )?.* (?:joined|(?:spooked|slid) into) the lobby!(?: <<<)?",

        // Hypixel SMP
        "\\s*?You can now create your own Hypixel SMP server!\\s*?",

        // Snow Particles in the Lobby
        "\\s*?.*For the best experience, click the text below to enable Snow\\s*Particles in this lobby!\\s*?.*Click to enable Snow Particles\\s*?",

        // mystery box
        "✦ .* found a .* Mystery Box!",
        "✦ .* found (a|an) .* in a (Holiday )?Mystery Box!",

        // mystery dust
        "✦ You earned \\d+ Mystery Dust!",

        // pet consumables
        "✦ You earned \\d+ Pet Consumables items!",

        // prototype
        "  ➤ You have reached your Hype limit! Add Hype to Prototype Lobby minigames by right-clicking with the Hype Diamond!",

        // prototype
        ".*Welcome to the Prototype Lobby.*",

        // hypixel tournament notifications
        ".*HYPIXEL is hosting a BED WARS DOUBLES tournament!.*",
        ".*HYPIXEL BED WARS DOUBLES tournament is live!.*",
        ".*HYPIXEL is hosting a TNT RUN tournament!.*",

        // other
        ".*You are still radiating with Generosity!.*",
    )

    // Warping
    private val warpingPatterns by miscPatternGroup.list(
        "warping",

        "Sending to server .*\\.\\.\\.",
        "Request join for Hub .*\\.\\.\\.",
        "Request join for Dungeon Hub #.*\\.\\.\\.",
        // warp portals on public islands
        // (Canvas Room – Flower House, Election Room – Community Center, Void Sepulture – The End)
        "Warped to .*!",

        "Warping...", "Warping you to your SkyBlock island...", "Warping using transfer token...",

        // visiting other players
        "Finding player...", "Sending a visit request...",
    )

    // Welcome
    private val welcomeMessages by miscPatternGroup.list(
        "welcome",
        "Welcome to Hypixel SkyBlock!",
    )

    // Guild & Event EXP
    /**
     * REGEX-TEST: You earned 2 GEXP from playing SkyBlock!
     * REGEX-TEST: You earned 2 GEXP + 210 Event EXP from playing SkyBlock!
     * REGEX-TEST: You earned 10 Event EXP from playing SkyBlock!
     */
    @Suppress("MaxLineLength")
    private val guildEventExpPatterns by eventPatternGroup.list(
        "guild-event",
        "You earned 0-9a-f][\\d,]+ (?:GEXP|Event EXP) (?:\\+ 0-9a-f][\\d,]+ Event EXP )?from playing SkyBlock!",
    )

    // Kill Combo
    /**
     * REGEX-TEST: +175 Kill Combo
     * REGEX-TEST: +5 Kill Combo +3% ✯ Magic Find
     */
    private val killComboPatterns by miscPatternGroup.list(
        "kill-combo",
        "\\+.* Kill Combo.*",
        "Your Kill Combo has expired! You reached a .* Kill Combo!",
        "\\+50 Kill Combo",
    )

    // Profile Join
    private val profileJoinPatterns by miscPatternGroup.list(
        "profile-join",
        "You are playing on profile: ",
        "Profile ID: ",
    )

    // OTHERS
    // Bazaar And AH Mini
    private val miniBazaarAndAHMessages by miscPatternGroup.list(
        "bazzar-and-ah-mini",
        "Putting item in escrow...",
        "Putting coins in escrow...",

        // Auction House
        "Setting up the auction...",
        "Processing purchase...",
        "Processing bid...",
        "Claiming BIN auction...",

        // Bazaar
        "\\[Bazaar] Submitting sell offer...",
        "\\[Bazaar] Submitting buy order...",
        "\\[Bazaar] Executing instant sell...",
        "\\[Bazaar] Executing instant buy...",
        "\\[Bazaar] Cancelling order...",
        "\\[Bazaar] Claiming order...",
        "\\[Bazaar] Putting goods in escrow...",

        // Bank
        "Depositing coins...",
        "Withdrawing coins...",
    )

    // Slayer
    private val slayerPatterns by RepoPattern.list(
        "slayer-quest",
        // start
        " {2}SLAYER QUEST STARTED!",
        " {3}» Slay .* Combat XP worth of .*.",

        // end
        " {2}SLAYER QUEST COMPLETE!",
        " {3}.*Slayer LVL 9 - LVL MAXED OUT!",
        " {3}» Talk to Maddox to claim your .* Slayer XP!",
        " {2}NICE! SLAYER BOSS SLAIN!", "You received kill credit for assisting on a slayer miniboss!",

        "✆ RING... .*",
        )

    // Slayer Drop
    @Suppress("MaxLineLength")
    private val slayerDropPatterns by RepoPattern.list(
        "slayer-drop",
        // Zombie
        // TODO merge patterns together. Just because old ones are designed poorly doesn't mean new ones need to be poor as well
        "RARE DROP! \\(.*x Revenant Viscera\\) .*",
        "RARE DROP! \\(Revenant Viscera\\) .*",
        "RARE DROP! \\(.*x Foul Flesh\\) .*",
        "RARE DROP! \\(Foul Flesh\\) .*",
        "RARE DROP! Golden Powder .*",
        "VERY RARE DROP! {2}\\(.* Pestilence Rune I\\) .*",
        "VERY RARE DROP! {2}\\(Revenant Catalyst\\) .*",
        "VERY RARE DROP! {2}\\(Undead Catalyst\\) .*",
        "VERY RARE DROP! {2}\\(◆ Pestilence Rune I\\) .*",

        // Tarantula
        "RARE DROP! Arachne's Keeper Fragment (.+)",
        "RARE DROP! Travel Scroll to Spider's Den Top of Nest (.+)",
        "VERY RARE DROP! {2}\\(◆ Bite Rune I\\) (.+)",
        "RARE DROP! \\((.+)x Toxic Arrow Poison\\) (.+)",
        "RARE DROP! \\(Toxic Arrow Poison\\) (.+)",
        "VERY RARE DROP! {2}\\(Bane of Arthropods VI\\) (.+)",

        // Enderman
        "RARE DROP! \\(.*x Twilight Arrow Poison\\) .*",
        "VERY RARE DROP! {2}\\(Mana Steal I\\) .*",
        "VERY RARE DROP! {2}\\(Sinful Dice\\) .*",
        "VERY RARE DROP! {2}\\(Null Atom\\) .*",
        "VERY RARE DROP! {2}\\(Transmission Tuner\\) .*",
        "VERY RARE DROP! {2}\\(Mana Steal I\\) .*",
        "VERY RARE DROP! {2}\\(◆ Endersnake Rune I\\) .*",
        "CRAZY RARE DROP! {2}\\(Pocket Espresso Machine\\) .*",
        "VERY RARE DROP! {2}\\(◆ End Rune I\\) .*",
        "VERY RARE DROP! {2}\\(Hazmat Enderman\\) .*",

        // Blaze
        "VERY RARE DROP! {2}\\(Wisp's Ice-Flavored Water I Splash Potion\\) .*",
        "RARE DROP! \\(Bundle of Magma Arrows\\) .*",
        "VERY RARE DROP! {2}\\(\\d+x (Glowstone|Blaze Rod|Magma Cream|Nether Wart) Distillate\\) .*",
    )

    // Useless Drop
    private val uselessDropPatterns by RepoPattern.list(
        "useless-drop",
        "RARE DROP! Enchanted Ender Pearl .*",
        "RARE DROP! Carrot .*",
        "RARE DROP! Potato .*",
        "RARE DROP! Machine Gun Bow .*",
        "RARE DROP! Earth Shard .*",
        "RARE DROP! Zombie Lord Chestplate .*",
        "RARE DROP! Enchanted Ender Pearl",
        "RARE DROP! Enchanted End Stone",
        "RARE DROP! Crystal Fragment",
    )

    // Legacy Items
    @Suppress("MaxLineLength")
    private val legacyItems by RepoPattern.list(
        "legacy-items",
        "You currently have one or more Legacy Items in your inventory or sacks that are no longer used throughout the game! Exchange them in the Legacy Trades menu, accessed through /legacytrades!",
    )

    // TODO update patterns for 1.21
    // Useless Notification
    private val uselessNotificationPatterns by RepoPattern.list(
        "useless-notification",
        "You tipped \\d+ players? in \\d+(?: different)? games?!",
        "Your previous Plasmaflux Power Orb was removed!",
        "You used your Mining Speed Boost Pickaxe Ability!",
        "Your Mining Speed Boost has expired!",
        "Mining Speed Boost is now available!",
        "You have just received 0 coins as interest in your personal bank account!",
        "Since you've been away you earned 0 coins as interest in your personal bank account!",
        "You have just received 0 coins as interest in your co-op bank account!",
    )

    // Party
    private val partyMessages by RepoPattern.list(
        "party",
        "-----------------------------------------------------",
    )

    // MONEY
    // Auction House
    private val auctionHouseMessages by RepoPattern.list(
        "auction-house",
        "-----------------------------------------------------",
        "Visit the Auction House to collect your item!",
    )

    // Bazaar
    private val bazaarPatterns by RepoPattern.list(
        "bazaar",
        "Buy Order Setup! .*x .* for .* coins.",
        "Sell Offer Setup! .*x .* for .* coins.",
        "Cancelled! Refunded .* coins from cancelling buy order!",
        "Cancelled! Refunded .*x .* from cancelling sell offer!",
    )

    // Winter Island
    private val winterIslandPatterns by RepoPattern.list(
        "winter-island",
        "☃ .* mounted a Snow Cannon!",
    )

    // Useless Warning
    private val uselessWarningMessages by RepoPattern.list(
        "useless-warning",
        "You are sending commands too fast! Please slow down.", // TODO prevent in the future
        "You can't use this while in combat!",
        "You can not modify your equipped armor set!",
        "Please wait a few seconds between refreshing!",
        "This item is not salvageable!", // TODO prevent in the future
        "Place a Dungeon weapon or armor piece above the anvil to salvage it!",
        "Whoa! Slow down there!",
        "Wait a moment before confirming!",
        "You cannot open the SkyBlock menu while in combat!",
        "Your radio is weak. Find another enjoyer to boost it.",
        "Your radio signal is strong!",
        "Your radio lost signal. There's too many enjoyers on this channel.",
    )

    // Annoying Spam
    @Suppress("MaxLineLength")
    private val annoyingSpamPatterns by RepoPattern.list(
        "annoying-spam",
        "Your Implosion hit .* for .* damage.",
        "Your Molten Wave hit .* for .* damage.",
        "Your Spirit Sceptre hit .* for .* damage.",
        "You need a tool with a Breaking Power of (\\d) to mine .*! Speak to Fragilis by the entrance to the Crystal Hollows to learn more!",
        "\nYouTube Premier Celebrate Hypixel's 12th Anniversary with a special Minecraft Animation, live now https://youtu.be/ikT631vQd8A\n",
        "There are blocks in the way!",
        "Your Blessing enchant got you double drops!",
        "You can't use the wardrobe in combat!",
        "GOOD CATCH! You found a Fish Bait.",
        "GOOD CATCH! You found a Grand Experience Bottle.",
        "GOOD CATCH! You found a Blessed Bait.",
        "GOOD CATCH! You found a Dark Bait.",
        "GOOD CATCH! You found a Light Bait.",
        "GOOD CATCH! You found a Hot Bait.",
        "GOOD CATCH! You found a Spooky Bait.",
        "\\[NPC] Jacob: My contest has started!",
        "Obtain a Booster Cookie from the community shop in the hub!",
        "Unknown command. Type \"/help\" for help. ('uhfdsolguhkjdjfhgkjhdfdlgkjhldkjhlkjhsldkjfhldshkjf')",
        "\\[SBE] Unable to download bin data. This may result in certain features not working!",
        "\\[NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.",
    )

    private val skymallMessages by RepoPattern.list(
        "skymall",
        "New day! Your Sky Mall buff changed!",
        "You can disable this messaging by toggling Sky Mall in your /hotm!",
    )

    private val lotteryMessages by RepoPattern.list(
        "lottery",
        "New day! Your Lottery buff changed!",
        "You can disable this messaging by toggling Lottery in your /hotf!",
    )

    /**
     * REGEX-TEST: [NPC] Jacob: Your Anita's Talisman is giving you +25 Carrot Fortune during the contest!
     */
    @Suppress("MaxLineLength")
    private val anitaFortunePattern by RepoPattern.pattern(
        "chat.jacobevent.accessory",
        "\\[NPC] Jacob: Your Anita's \\w+ is giving you \\+\\d{1,2}${SkyblockStat.FARMING_FORTUNE.hypixelIcon} .+ Fortune during the contest!",
    )

    // Winter Gift
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

    private val fireSalePattern by RepoPattern.pattern(
        "chat.firesale",
        "A FIRE SALE A(?:\\n|.)*",
    )
    private val fireSalePatterns by RepoPattern.list(
        "♨ Fire Sales for .* are starting soon!",
        "\\s*♨ .* (?:Skin|Rune|Dye) (?:for a limited time )?\\(.* left\\)(?:|!)",
        "♨ Visit the Community Shop in the next .* to grab yours! \\[WARP]",
        "♨ A Fire Sale for .* is starting soon!",
        "♨ Fire Sales? for .* ended!",
        " {3}♨ And \\d+ more!",
    )
    private val eventPatterns by RepoPattern.list(
        "(?:)? +You are now Event Level *!",
        "(?:)? +You earned * Event Silver!",
        "(?:)? +# LEVEL UP! #",
    )
    private val factoryUpgradePatterns by RepoPattern.list(
        ".* has been promoted to \\[.*] *!",
        "Your Rabbit Barn capacity has been increased to .* Rabbits!",
        "You will now produce .* Chocolate per click!",
        "You upgraded to .*?!",
    )

    /**
     * REGEX-TEST: SACRIFICE! [MVP++] Mikecraft1224 turned Young Dragon Boots into 40 Dragon Essence!
     * REGEX-TEST: BONUS LOOT! They also received Ritual Residue from their sacrifice!
     */
    private val sacrificePatterns by RepoPattern.list(
        "SACRIFICE! .* turned .* into .* Dragon Essence!",
        "BONUS LOOT! They also received .* from their sacrifice!",
    )
    private val powderMiningMessages by RepoPattern.list(
        "You uncovered a treasure chest!",
        "You received 1 Wishing Compass.",
        "You received 1 Ascension Rope.",
        // Jungle
        "You received 1 Oil Barrel.",
        // Useful, maybe in another chat
        "You have successfully picked the lock on this chest!",
    )
    private val fireSaleMessages by RepoPattern.list(
        "A FIRE SALE A",
        "♨ Selling multiple items for a limited time!",
    )
    private val eventMessage by RepoPattern.list(
        "▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
    )

    /**
     * REGEX-TEST: RARE REWARD! Leebys found a Recombobulator 3000 in their Obsidian Chest!
     */
    private val rareDropsMessages by RepoPattern.list(
        "RARE REWARD! .* found a .* in their .* Chest!",
    )

    // &r&6Your &r&aMage &r&6stats are doubled because you are the only player using this class!&r
    private val soloClassPatterns by RepoPattern.list(
        "Your (Healer|Mage|Berserk|Archer|Tank) stats are doubled because you are the only player using this class!",
    )

    private val soloStatsPatterns by RepoPattern.list(
        "\\[(Healer|Mage|Berserk|Archer|Tank)].*",
    )

    // &r&dGenevieve the Fairy&r&f: You killed me! Take this &r&6Revive Stone &r&fso that my death is not in vain!&r
    private val fairyPatterns by RepoPattern.list(
        "[\\w']+ the Fairy: You killed me! Take this Revive Stone so that my death is not in vain!",
        "[\\w']+ the Fairy: You killed me! I'll revive you so that my death is not in vain!",
        "[\\w']+ the Fairy: Have a great life!",
    )

    // a>>   Achievement Unlocked: Agile   <<a
    private val achievementGetPatterns by RepoPattern.list(
        ".>> {3}Achievement Unlocked: .* {3}<<.",
    )

    /**
     * REGEX-TEST: Started parkour cocoa!
     * REGEX-TEST: Finished parkour cocoa in 12:34.567!
     * REGEX-TEST: Reached checkpoint #4 for parkour cocoa!
     * REGEX-TEST: Wrong checkpoint for parkour cocoa!
     * REGEX-TEST: You haven't reached all checkpoints for parkour cocoa!
     */
    private val parkourPatterns by RepoPattern.list(
        "Started parkour .*!",
        "Finished parkour .* in .*!",
        "Reached checkpoint #.* for parkour .*!",
        "Wrong checkpoint for parkour .*!",
        "You haven't reached all checkpoints for parkour .*!",
    )

    /**
     * REGEX-TEST: Cancelled parkour! You cannot fly.
     * REGEX-TEST: Cancelled parkour! You cannot use item abilities.
     * REGEX-TEST: Cancelled parkour!
     */
    private val parkourCancelMessages by RepoPattern.list(
        "Cancelled parkour! You cannot fly.",
        "Cancelled parkour! You cannot use item abilities.",
        "Cancelled parkour!",
    )

    /**
     ** REGEX-TEST: Warped from the tpPadOne to the tpPadTwo!
     */
    private val teleportPadPatterns by RepoPattern.list(
        "Warped from the .* to the .*!",
    )

    // This Teleport Pad does not have a destination set!
    private val teleportPadMessages by RepoPattern.list(
        "This Teleport Pad does not have a destination set!",
    )

    // [NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.
    private val MasterChefPatterns by RepoPattern.list(
        "\\[NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.",
    )

    // [NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.
    private val MasterChefMessages by RepoPattern.list(
        "[NPC] Feast Chef Ted: Thanks for the donation! I've added a Kernel to your purse.",
    )

    /**
     ** REGEX-TEST: You haven't claimed your Summer Rewards yet!
     ** REGEX-TEST: Talk to the Summer Sloth in the Hub!
     ** REGEX-TEST: Talk to the Random NPC in the Forbidden Zone!
     */
    private val rewardBundlePatterns by miscPatternGroup.list(
        "seasonal-bundles",
        "(?:)*You haven't claimed your (?:)*\\w+ Rewards (?:)*yet!",
        "(?:)*Talk to the (?:)*.+(?:)*in the (?:)*.+(?:)*!",
    )

    /**
     ** REGEX-TEST: You cannot damage a tree while it is regenerating!
     ** REGEX-TEST: The toughness of this tree is way too high!
     */
    private val unmineableTreePatterns by foragingPatternGroup.list(
        "unmineable-tree",
        "(?:)*You cannot damage a tree while it is regenerating!",
        "(?:)*The toughness of this tree is way too high!",
    )

    /**
     ** REGEX-TEST: Mochibear ate too much and passed out! You caught it!
     ** REGEX-TEST: You caught yourself an invisibug! The shard was sent to your Hunting Box!
     ** REGEX-TEST: The Frog is exhausted...
     */
    private val redundantShardsPatterns by huntingPatternGroup.list(
        "redundant-comments",
        "(?:)*Mochibear ate too much and passed out! You caught it!",
        "(?:)*You caught yourself an invisibug! The shard was sent to your Hunting Box!",
        "(?:)*The Frog is exhausted\\.\\.\\.",
    )

    /**
     * REGEX-TEST: [NPC] Swoop: Wow! I forgot to tell you, monsters around here can only take damage from Axes!
     */
    private val swoopAxePattern by huntingPatternGroup.pattern(
        "swoop-axe-message",
        "\\[NPC] Swoop: Wow! I forgot to tell you, monsters around here can only take damage from Axes!",
    )

    /**
     * REGEX-TEST: HOPPITY'S HUNT A Chocolate Dinner Egg has appeared!
     * REGEX-TEST: HOPPITY'S HUNT A Chocolate Déjeuner Egg has appeared!
     * REGEX-TEST: HOPPITY'S HUNT A Chocolate Brunch Egg has appeared!
     */
    private val hoppityAppearPattern by eventPatternGroup.pattern(
        "hoppity-egg-appear",
        "HOPPITY'S HUNT A .* has appeared!",
    )

    @Suppress("MaxLineLength")
    private val hoppityBeginPattern by eventPatternGroup.pattern(
        "hoppity-begin",
        "Hoppity's Hunt has begun! Help Hoppity find his Chocolate Rabbit Eggs across SkyBlock each day during the Spring!",
    )

    private val patternsMap: Map<String, List<Pattern>> = mapOf(
        "lobby" to lobbyPatterns,
        "warping" to warpingPatterns,
        "guild_event_exp" to guildEventExpPatterns,
        "kill_combo" to killComboPatterns,
        "slayer" to slayerPatterns,
        "slayer_drop" to slayerDropPatterns,
        "useless_drop" to uselessDropPatterns,
        "legacy_items" to legacyItems,
        "useless_notification" to uselessNotificationPatterns,
        "money" to bazaarPatterns,
        "winter_island" to winterIslandPatterns,
        "annoying_spam" to annoyingSpamPatterns,
        "winter_gift" to winterGiftPatterns,
        "fire_sale" to fireSalePatterns,
        "event" to eventPatterns,
        "factory_upgrade" to factoryUpgradePatterns,
        "sacrifice" to sacrificePatterns,
        "rare_drops" to rareDropsMessages,
        "solo_class" to soloClassPatterns,
        "solo_stats" to soloStatsPatterns,
        "fairy" to fairyPatterns,
        "achievement_get" to achievementGetPatterns,
        "parkour" to parkourPatterns,
        "teleport_pads" to teleportPadPatterns,
        "masterchef" to MasterChefPatterns,
    )

    private val repoPatternsMap: Map<String, List<Pattern>> = mapOf(
        "reward_bundles" to rewardBundlePatterns,
        "redundant_hunting" to redundantShardsPatterns,
        "unmineable_tree" to unmineableTreePatterns,
        "swoop_axe" to listOf(swoopAxePattern),
        "hoppity_appear" to listOf(hoppityAppearPattern),
        "hoppity_begin" to listOf(hoppityBeginPattern),
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
        "event" to eventMessage,
        "skymall" to skymallMessages,
        "lottery" to lotteryMessages,
        "parkour" to parkourCancelMessages,
        "teleport_pads" to teleportPadMessages,
        "masterchef" to MasterChefMessages,
    )

    private val messagesContainsMap: Map<String, List<String>> = mapOf(
        "lobby" to lobbyMessagesContains,
    )

    private val messagesStartsWithMap: Map<String, List<String>> = mapOf(
        "slayer" to slayerMessageStartWith,
        "profile_join" to profileJoinMessageStartsWith,
    )
    // </editor-fold>

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        var blockReason = block(event.message)
        if (blockReason == null && config.powderMining.enabled) blockReason = powderMiningBlock(event)
        if (blockReason == null && config.crystalNucleus.enabled) blockReason = crystalNucleusBlock(event)

        event.blockedReason = blockReason ?: return
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Modify) {
        if (config.powderMining.enabled) powderMiningBlock(event)
        if (config.crystalNucleus.enabled) crystalNucleusBlock(event)
    }

    /**
     * Checks if the message should be blocked
     * @param message The message to check
     * @return The reason why the message was blocked, empty if not blocked
     */
    @Suppress("CyclomaticComplexMethod", "MaxLineLength")
    private fun block(message: String): String? = when {
        config.hypixelHub && message.isPresent("lobby") -> "lobby"
        config.empty && StringUtils.isEmpty(message) -> "empty"
        config.warping && message.isPresent("warping") -> "warping"
        config.welcome && message.isPresent("welcome") -> "welcome"
        config.guildEventExp && message.isPresent("guild_event_exp") -> "guild_event_exp"
        config.killCombo && message.isPresent("kill_combo") -> "kill_combo"
        config.profileJoin && message.isPresent("profile_join") -> "profile_join"
        config.parkour && message.isPresent("parkour") -> "parkour"
        config.teleportPads && message.isPresent("teleport_pads") -> "teleport_pads"
        config.masterChef && MasterChefPatterns.matches(message) -> "masterchef"

        config.hideAlphaAchievements && HypixelData.hypixelAlpha && message.isPresent("achievement_get") -> "achievement_get"

        config.others && isOthers(message) -> othersMsg

        config.winterGift && message.isPresent("winter_gift") -> "winter_gift"

        // TODO need proper solution to hide empty messages in event text
        config.eventLevelUp && (message.isPresent("event")) -> "event"

        config.fireSale && (fireSalePattern.matches(message) || message.isPresent("fire_sale")) -> "fire_sale"
        config.rewardBundles && message.isPresent("reward_bundles") -> "reward_bundles"
        config.factoryUpgrade && message.isPresent("factory_upgrade") -> "factory_upgrade"
        config.hoppityEggs && message.isPresent("hoppity_appear") -> "hoppity_appear"
        config.hoppityBegun && message.isPresent("hoppity_begin") -> "hoppity_begin"
        config.sacrifice && message.isPresent("sacrifice") -> "sacrifice"
        generalConfig.hideJacob && !GardenApi.inGarden() && anitaFortunePattern.matches(message) -> "jacob_event"
        generalConfig.hideSkyMall && !IslandTypeTag.MINING.isInIsland() && message.isPresent("skymall") -> "skymall"
        generalConfig.hideLottery && !IslandTypeTag.FORAGING.isInIsland() && message.isPresent("lottery") -> "lottery"
        dungeonConfig.rareDrops && message.isPresent("rare_drops") -> "rare_drops"
        dungeonConfig.soloClass && DungeonApi.inDungeon() && message.isPresent("solo_class") -> "solo_class"
        dungeonConfig.soloStats && DungeonApi.inDungeon() && message.isPresent("solo_stats") -> "solo_stats"
        dungeonConfig.fairy && DungeonApi.inDungeon() && message.isPresent("fairy") -> "fairy"
        foragingConfig.unmineable && IslandTypeTag.FORAGING_CUSTOM_TREES.isInIsland() && message.isPresent("unmineable_tree") -> "unmineable_tree"
        huntingConfig.redundantComments && IslandType.GALATEA.isInIsland() && message.isPresent("redundant_hunting") -> "redundant_hunting"
        huntingConfig.swoopAxeMessage && message.isPresent("swoop_axe") -> "swoop_axe"
        config.gardenNoPest && GardenApi.inGarden() && PestApi.noPestsChatPattern.matches(message) -> "garden_pest"
        config.legacyItemsWarning && message.isPresent("legacy_items") -> "legacy_items"

        else -> null
    }

    /**
     * Checks if the message is a blocked powder mining message, as defined in PowderMiningChatFilter.
     * Will return a resultant blocking code
     * @param event The event to check
     * @return Block reason if applicable
     * @see block
     */
    private fun powderMiningBlock(event: SkyHanniChatEvent.Allow): String? {
        val powderMiningMatchResult = PowderMiningChatFilter.block(event.message)
        if (powderMiningMatchResult == "no_filter") {
            return null
        }
        return powderMiningMatchResult
    }

    /**
     * Checks if the message is a blocked powder mining message, as defined in PowderMiningChatFilter.
     * Will modify un-filtered Mining reward
     * @param event The event to check
     * @see block
     */
    private fun powderMiningBlock(event: SkyHanniChatEvent.Modify) {
        val powderMiningMatchResult = PowderMiningChatFilter.block(event.message)
        if (powderMiningMatchResult == "no_filter") {
            genericMiningRewardMessage.matchMatcher(event.message) {
                val reward = groupOrEmpty("reward")
                val amountFormat = groupOrNull("amount")?.let {
                    "+ $it"
                } ?: "+"
                event.replaceComponent("$amountFormat $reward".asComponent(), "powder_gain")
            }
        }
    }

    /**
     * Checks if the message is a blocked Crystal Nucleus Run message, as defined in CrystalNucleusChatFilter.
     * Will conditionally return a blocking code
     * @param event The event to check
     * @return Block reason if applicable
     * @see block
     */
    private fun crystalNucleusBlock(event: SkyHanniChatEvent.Allow): String? {
        val blockCode = CrystalNucleusChatFilter.block(event.message)?.getPair()?.first
        blockCode?.let { return it }
        return null
    }

    /**
     * Checks if the message is a blocked Crystal Nucleus Run message, as defined in CrystalNucleusChatFilter.
     * Will conditionally modify/compact messages in some cases
     * @param event The event to check
     * @see block
     */
    private fun crystalNucleusBlock(event: SkyHanniChatEvent.Modify) {
        val newMessage = CrystalNucleusChatFilter.block(event.message)?.getPair()?.second
        newMessage?.let {
            event.replaceComponent(it.asComponent(), "nuc_run")
        }
    }

    private var othersMsg: String? = null

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
            else -> null
        }
        return othersMsg != null
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
        event.move(52, "chat.filterType.powderMining", "chat.filterType.powderMiningFilter.enabled")
        event.transform(53, "chat.filterType.powderMiningFilter.gemstoneFilterConfig") { element ->
            element.asJsonObject.apply {
                entrySet().forEach { (key, value) ->
                    if (value.asString == "FINE_ONLY") addProperty(key, "FINE_UP")
                }
            }
        }
        event.move(61, "chat.filterType.powderMiningFilter", "chat.filterType.powderMining")
        event.move(61, "chat.filterType.gemstoneFilterConfig", "chat.filterType.powderMining.gemstone")
        event.move(107, "chat.filterType.guildExp", "chat.filterType.guildEventExp")
    }
}
