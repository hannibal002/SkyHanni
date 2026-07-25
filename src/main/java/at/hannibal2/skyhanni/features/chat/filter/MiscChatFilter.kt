package at.hannibal2.skyhanni.features.chat.filter

import at.hannibal2.skyhanni.data.HypixelData
import at.hannibal2.skyhanni.utils.StringUtils

@Suppress("MaxLineLength")
object MiscChatFilter : ChatFilterGroup() {
    private val patternGroup = ChatFilterManager.chatFilterGroup.group("hypixel-misc")
    private val config get() = ChatFilterManager.config

    override val filters: Set<ChatFilter> = setOf(
        EmptyFilter,
        WelcomeFilter,
        LobbyFilter,
        WarpingFilter,
        KillComboFilter,
        ProfileJoinFilter,
        MiniBazaarAndAHFilter,
        AchievementGetFilter,
        ParkourFilter,
        TeleportPadFilter,
        UselessDropFilter,
        LegacyItemsFilter,
        UselessNotificationFilter,
        PartyFilter,
        AuctionHouseFilter,
        BazaarFilter,
        UselessWarningFilter,
        AnnoyingSpamFilter,
        RewardBundleFilter,
        SacrificeFilter,
    )

    object EmptyFilter : ChatFilter {
        private val activation = Activation.Config(config.empty)

        init {
            activation.bind(
                onEnable = {
                    ChatFilterManager.register(this)
                },
                onDisable = {
                    ChatFilterManager.unregister(this)
                },
            )
        }

        override fun block(message: String): String? {
            if (!StringUtils.isEmpty(message)) return null
            return "empty"
        }
    }

    object WelcomeFilter : RegexChatFilter("welcome", config.welcome) {
        override val patterns by patternGroup.list(
            "welcome",
            "Welcome to Hypixel SkyBlock!",
        )
    }

    object LobbyFilter : RegexChatFilter("lobby", config.hypixelHub) {
        override val patterns by patternGroup.list(
            "lobby",

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
            "\\s+➤ You have reached your Hype limit! Add Hype to Prototype Lobby minigames by right-clicking with the Hype Diamond!",

            // prototype
            ".*Welcome to the Prototype Lobby.*",

            // hypixel tournament notifications
            ".*HYPIXEL is hosting a BED WARS DOUBLES tournament!.*",
            ".*HYPIXEL BED WARS DOUBLES tournament is live!.*",
            ".*HYPIXEL is hosting a TNT RUN tournament!.*",

            // other
            ".*You are still radiating with Generosity!.*",
        )
    }

    object WarpingFilter : RegexChatFilter("warping", config.warping) {
        override val patterns by patternGroup.list(
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
    }

    object KillComboFilter : RegexChatFilter("kill_combo", config.killCombo) {
        override val patterns by patternGroup.list(
            "kill-combo",
            "\\+.* Kill Combo.*",
            "Your Kill Combo has expired! You reached a .* Kill Combo!",
            "\\+50 Kill Combo",
        )
    }

    object ProfileJoinFilter : RegexChatFilter("profile_join", config.profileJoin) {
        override val patterns by patternGroup.list(
            "profile_join",
            "You are playing on profile: ",
            "Profile ID: ",
        )
    }

    object MiniBazaarAndAHFilter : RegexChatFilter("bazzar_and_ah_mini", config.others) {
        override val patterns by patternGroup.list(
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
    }

    @Suppress("RepoPatternRegexTestMissing")
    object AchievementGetFilter : RegexChatFilter("achievement_get", config.hideAlphaAchievements) {
        /**
         * WRAPPED-REGEX_TEST: ">>>   Achievement Unlocked: The Beginning   <<<"
         */
        override val patterns by patternGroup.list(
            "achievement-get",
            ".>> {3}Achievement Unlocked: .* {3}<<.",
        )

        override fun block(message: String): String? {
            if (!HypixelData.hypixelAlpha) return null
            return super.block(message)
        }
    }

    object ParkourFilter : RegexChatFilter("parkour", config.parkour) {
        override val patterns by patternGroup.list(
            "parkour",
            "Started parkour .*!",
            "Finished parkour .* in .*!",
            "Reached checkpoint #.* for parkour .*!",
            "Wrong checkpoint for parkour .*!",
            "You haven't reached all checkpoints for parkour .*!",
            "Cancelled parkour! You cannot fly.",
            "Cancelled parkour! You cannot use item abilities.",
            "Cancelled parkour!",
        )
    }

    object TeleportPadFilter : RegexChatFilter("teleport_pad", config.teleportPads) {
        override val patterns by patternGroup.list(
            "teleport-pad",
            "Warped from the .* to the .*!",
            "This Teleport Pad does not have a destination set!",
        )
    }

    object UselessDropFilter : RegexChatFilter("useless_drop", config.others) {
        override val patterns by patternGroup.list(
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
    }

    object LegacyItemsFilter : RegexChatFilter("legacy_items", config.legacyItemsWarning) {
        override val patterns by patternGroup.list(
            "legacy-items",
            "You currently have one or more Legacy Items in your inventory or sacks that are no longer used throughout the game! Exchange them in the Legacy Trades menu, accessed through /legacytrades!",
        )
    }

    object UselessNotificationFilter : RegexChatFilter("useless_notification", config.others) {
        // TODO update patterns for 1.21
        override val patterns by patternGroup.list(
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
    }

    object PartyFilter : RegexChatFilter("party", config.others) {
        override val patterns by patternGroup.list(
            "party",
            "-----------------------------------------------------",
        )
    }

    object AuctionHouseFilter : RegexChatFilter("auction_house", config.others) {
        override val patterns by patternGroup.list(
            "auction-house",
            "-----------------------------------------------------",
            "Visit the Auction House to collect your item!",
        )
    }

    object BazaarFilter : RegexChatFilter("bazaar", config.others) {
        override val patterns by patternGroup.list(
            "bazaar",
            "Buy Order Setup! .*x .* for .* coins.",
            "Sell Offer Setup! .*x .* for .* coins.",
            "Cancelled! Refunded .* coins from cancelling buy order!",
            "Cancelled! Refunded .*x .* from cancelling sell offer!",
        )
    }

    object UselessWarningFilter : RegexChatFilter("useless_warning", config.others) {
        override val patterns by patternGroup.list(
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
    }

    object AnnoyingSpamFilter : RegexChatFilter("annoying_spam", config.others) {
        override val patterns by patternGroup.list(
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
    }

    object RewardBundleFilter : RegexChatFilter("seasonal_bundles", config.others) {
        /**
         * REGEX-TEST: You haven't claimed your Summer Rewards yet!
         * REGEX-TEST: Talk to the Summer Sloth in the Hub!
         * REGEX-TEST: Talk to the Random NPC in the Forbidden Zone!
         */
        override val patterns by patternGroup.list(
            "seasonal-bundles",
            "You haven't claimed your \\w+ Rewards yet!",
            "Talk to the .+in the .+!",
        )
    }


    object SacrificeFilter : RegexChatFilter("sacrifice", config.sacrifice) {
        /**
         * REGEX-TEST: SACRIFICE! [MVP++] Mikecraft1224 turned Young Dragon Boots into 40 Dragon Essence!
         * REGEX-TEST: BONUS LOOT! They also received Ritual Residue from their sacrifice!
         */
        override val patterns by patternGroup.list(
            "sacrifice",
            "SACRIFICE! .* turned .* into .* Dragon Essence!",
            "BONUS LOOT! They also received .* from their sacrifice!",
        )
    }
}
